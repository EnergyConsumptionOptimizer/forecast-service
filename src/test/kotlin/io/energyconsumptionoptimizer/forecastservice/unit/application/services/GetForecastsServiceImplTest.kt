package io.energyconsumptionoptimizer.forecastservice.unit.application.services

import arrow.core.raise.either
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.aForecast
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.application.services.GetForecastsServiceImpl
import io.energyconsumptionoptimizer.forecastservice.domain.entity.Forecast
import io.energyconsumptionoptimizer.forecastservice.domain.ports.ForecastRepository
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk

class GetForecastsServiceImplTest :
    DescribeSpec({

        isolationMode = IsolationMode.InstancePerLeaf

        val electricityForecast = aForecast(UtilityType.ELECTRICITY)
        val gasForecast = aForecast(UtilityType.GAS)

        val repository = mockk<ForecastRepository>(relaxed = true)

        val service = GetForecastsServiceImpl(repository)

        coEvery { repository.findByUtility(any()) } returns null
        coEvery { repository.findAll() } returns emptyList()

        describe("GetForecastsServiceImpl") {

            describe("getAll") {

                context("when multiple forecasts exist") {
                    it("should return all forecasts mapped to DTOs") {
                        coEvery { repository.findAll() } returns listOf(electricityForecast, gasForecast)

                        val result = either { service.getAll() }.shouldBeRight()

                        result shouldHaveSize 2
                        result[0].utilityType shouldBe electricityForecast.utilityType
                        result[1].utilityType shouldBe gasForecast.utilityType
                    }
                }

                context("when no forecasts exist") {
                    it("should return an empty list") {
                        val result = either { service.getAll() }.shouldBeRight()

                        result.shouldBeEmpty()
                    }
                }
            }

            describe("getByUtility") {

                context("when forecast exists for the requested utility") {
                    it("should return the forecast mapped to DTO") {
                        coEvery { repository.findByUtility(UtilityType.ELECTRICITY) } returns electricityForecast

                        val result =
                            either {
                                service.getByUtility(UtilityType.ELECTRICITY)
                            }.shouldBeRight()

                        result.utilityType shouldBe UtilityType.ELECTRICITY
                        result.dataPoints shouldHaveSize electricityForecast.series.points.size
                    }
                }

                context("when no forecast exists for the requested utility") {
                    it("should raise ForecastNotFound") {
                        val error =
                            either {
                                service.getByUtility(UtilityType.WATER)
                            }.shouldBeLeft()

                        error.shouldBeInstanceOf<ApplicationError.ForecastNotFound>()
                        error.utilityType shouldBe UtilityType.WATER
                    }
                }
            }
        }
    })
