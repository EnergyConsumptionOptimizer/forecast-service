package io.energyconsumptionoptimizer.forecastingservice.application.usecases

import io.energyconsumptionoptimizer.forecastingservice.domain.value.PeriodType
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType
import io.energyconsumptionoptimizer.forecastingservice.utils.fakes.FakeForecastRepository
import io.energyconsumptionoptimizer.forecastingservice.utils.fakes.FakeForecastingAlgorithm
import io.energyconsumptionoptimizer.forecastingservice.utils.fakes.FakeHistoricalDataProvider
import io.energyconsumptionoptimizer.forecastingservice.utils.fakes.FakeThresholdNotifier
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ComputeForecastUseCaseTest :
    BehaviorSpec({
        val repository = FakeForecastRepository()
        val historicalDataProvider = FakeHistoricalDataProvider()
        val forecastingAlgorithm = FakeForecastingAlgorithm()
        val thresholdNotifier = FakeThresholdNotifier()
        val useCase =
            ComputeForecastUseCase(
                repository = repository,
                historicalDataProvider = historicalDataProvider,
                forecastingAlgorithm = forecastingAlgorithm,
                thresholdNotifier = thresholdNotifier,
            )

        afterContainer {
            repository.clear()
            thresholdNotifier.notifications.clear()
        }

        Given("a forecast computation request for water utility") {
            When("computing a new forecast") {
                val forecast = useCase.compute(UtilityType.WATER)

                Then("it should adapt forecast horizon to 30% of available historical data") {
                    forecast.forecastedDataPoints shouldHaveSize 21 // 30% of 70
                }

                Then("it should produce forecast for the requested utility type") {
                    forecast.utilityType shouldBe UtilityType.WATER
                }

                Then("it should store the forecast in the repository") {
                    repository.findById(forecast.id) shouldBe forecast
                }
            }
        }

        Given("a forecast computation request for gas utility") {
            When("computing the forecast") {
                useCase.compute(UtilityType.GAS)

                Then("it should send aggregated consumption data to threshold service") {
                    thresholdNotifier.notifications shouldHaveSize 1
                    val (utilityType, aggregations) = thresholdNotifier.notifications.first()

                    utilityType shouldBe UtilityType.GAS
                    aggregations shouldHaveSize 3
                    aggregations shouldContain (PeriodType.ONE_DAY to 100.0)
                    aggregations shouldContain (PeriodType.ONE_WEEK to 721.0)
                    aggregations shouldContain (PeriodType.ONE_MONTH to 2310.0)
                }
            }
        }

        Given("an existing forecast for water utility") {
            When("computing another forecast for the same utility") {
                val first = useCase.compute(UtilityType.WATER)
                val second = useCase.compute(UtilityType.WATER)

                Then("it should replace the old forecast with the new one") {
                    first.id shouldNotBe second.id
                    repository.findById(first.id) shouldBe null
                    repository.findById(second.id) shouldBe second
                }
            }
        }

        Given("a request to compute forecasts for all utilities") {
            When("executing the computation") {
                val forecasts = useCase.computeAll()

                Then("it should generate forecasts for every utility type") {
                    forecasts shouldHaveSize UtilityType.entries.size
                    forecasts.map { it.utilityType }.toSet() shouldBe UtilityType.entries.toSet()
                }
            }
        }
    })
