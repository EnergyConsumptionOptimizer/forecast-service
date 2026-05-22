package io.energyconsumptionoptimizer.forecastservice.unit.application.services

import arrow.core.raise.Raise
import arrow.core.raise.either
import io.energyconsumptionoptimizer.forecastservice.ApplicationFixtures
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.forecastPoint
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.today
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.BusinessMetricsPort
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.EventPublisher
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.ForecastAlgorithm
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.HistoricalDataProvider
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.UnitOfWork
import io.energyconsumptionoptimizer.forecastservice.application.services.ComputeForecastServiceImpl
import io.energyconsumptionoptimizer.forecastservice.domain.entity.Forecast
import io.energyconsumptionoptimizer.forecastservice.domain.event.DomainEvent
import io.energyconsumptionoptimizer.forecastservice.domain.ports.ForecastRepository
import io.energyconsumptionoptimizer.forecastservice.domain.value.ForecastedDataSeries
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot

class ComputeForecastServiceImplTest :
    DescribeSpec({

        isolationMode = IsolationMode.InstancePerLeaf

        val anyUtility = UtilityType.ELECTRICITY

        val repository = mockk<ForecastRepository>(relaxed = true)
        val historicalDataProvider = mockk<HistoricalDataProvider>()
        val forecastAlgorithm = mockk<ForecastAlgorithm>()
        val uow = mockk<UnitOfWork>()
        val eventPublisher = mockk<EventPublisher>(relaxed = true)
        val metrics = mockk<BusinessMetricsPort>(relaxed = true)

        val service =
            ComputeForecastServiceImpl(
                repository,
                historicalDataProvider,
                forecastAlgorithm,
                uow,
                eventPublisher,
                metrics,
            )

        val actionSlot = slot<suspend () -> Unit>()
        ApplicationFixtures.setupExecuteTransactionally(uow, actionSlot)

        coEvery { repository.findByUtility(any()) } returns null

        ApplicationFixtures.mockForecastAlgorithmReturns(
            forecastAlgorithm,
            ApplicationFixtures.validAlgorithmPredictionDto,
        )

        ApplicationFixtures.mockHistoricalDataProviderReturns(
            historicalDataProvider,
            ApplicationFixtures.validHistoricalDataDto,
        )

        describe("ComputeForecastServiceImpl") {

            describe("compute") {

                context("given valid historical data and successful algorithm prediction") {
                    it("successfully computes the forecast, saves it via outbox, and records metrics") {
                        either { service.compute(anyUtility) }.shouldBeRight()

                        coVerify(exactly = 1) { repository.save(any<Forecast>()) }
                        coVerify(exactly = 1) { metrics.recordForecastComputation() }

                        coVerify(exactly = 1) {
                            with(any<Raise<ApplicationError.EventPublishFailed>>()) {
                                eventPublisher.publish(any<DomainEvent>())
                            }
                        }
                        coVerify(exactly = 1) {
                            with(any<Raise<ApplicationError.TransactionFailed>>()) {
                                uow.executeTransactionally(any())
                            }
                        }
                    }
                }

                context("given an existing forecast for the utility") {
                    it("recomputes the existing forecast instead of creating a new one") {
                        val series =
                            either {
                                ForecastedDataSeries.of(listOf(forecastPoint(today, 10.0)))
                            }.shouldBeRight()

                        val existingForecast =
                            Forecast.restore(
                                anyUtility,
                                series,
                                kotlin.time.Clock.System
                                    .now(),
                            )
                        coEvery { repository.findByUtility(anyUtility) } returns existingForecast

                        either { service.compute(anyUtility) }.shouldBeRight()

                        coVerify(exactly = 1) { repository.save(existingForecast) }
                    }
                }

                context("when domain validation fails (e.g. empty historical data)") {
                    it("raises an ApplicationError.Domain and rolls back/stops execution") {
                        ApplicationFixtures.mockHistoricalDataProviderReturns(
                            historicalDataProvider,
                            ApplicationFixtures.emptyHistoricalDataDto,
                        )

                        val error = either { service.compute(anyUtility) }.shouldBeLeft()

                        error.shouldBeInstanceOf<ApplicationError.Domain>()
                        coVerify(exactly = 0) { repository.save(any()) }

                        coVerify(exactly = 0) {
                            with(any<Raise<ApplicationError.EventPublishFailed>>()) {
                                eventPublisher.publish(any())
                            }
                        }
                    }
                }
            }

            describe("computeAll") {

                context("when all utilities compute successfully") {
                    it("completes without raising errors and processes all utilities") {
                        either { service.computeAll() }.shouldBeRight()

                        coVerify(exactly = UtilityType.entries.size) { repository.save(any<Forecast>()) }
                    }
                }

                context("when some utilities fail to compute") {
                    it("raises BatchComputationFailed containing both successes and failures") {
                        coEvery {
                            with(any<Raise<ApplicationError.DataProviderError>>()) {
                                historicalDataProvider.fetchByUtility(UtilityType.GAS)
                            }
                        } returns ApplicationFixtures.emptyHistoricalDataDto

                        coEvery {
                            with(any<Raise<ApplicationError.DataProviderError>>()) {
                                historicalDataProvider.fetchByUtility(neq(UtilityType.GAS))
                            }
                        } returns ApplicationFixtures.validHistoricalDataDto

                        val error = either { service.computeAll() }.shouldBeLeft()

                        error.shouldBeInstanceOf<ApplicationError.BatchComputationFailed>()

                        val expectedSuccesses = UtilityType.entries.filter { it != UtilityType.GAS }
                        error.successes shouldContainExactly expectedSuccesses

                        error.failures shouldContainKey UtilityType.GAS
                        error.failures[UtilityType.GAS].shouldBeInstanceOf<ApplicationError.Domain>()
                    }
                }

                context("when all utilities fail to compute") {
                    it("raises BatchComputationFailed with empty successes and all mapped failures") {
                        ApplicationFixtures.mockHistoricalDataProviderReturns(
                            historicalDataProvider,
                            ApplicationFixtures.emptyHistoricalDataDto,
                        )

                        val error = either { service.computeAll() }.shouldBeLeft()

                        error.shouldBeInstanceOf<ApplicationError.BatchComputationFailed>()
                        error.successes.size shouldBe 0
                        error.failures.size shouldBe UtilityType.entries.size
                    }
                }
            }
        }
    })
