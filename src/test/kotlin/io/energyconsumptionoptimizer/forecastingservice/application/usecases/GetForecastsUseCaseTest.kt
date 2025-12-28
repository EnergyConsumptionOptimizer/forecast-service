package io.energyconsumptionoptimizer.forecastingservice.application.usecases

import io.energyconsumptionoptimizer.forecastingservice.domain.ForecastedConsumption
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ConsumptionValue
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastedDataPoint
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType
import io.energyconsumptionoptimizer.forecastingservice.utils.fakes.FakeForecastRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class GetForecastsUseCaseTest :
    BehaviorSpec({
        val repository = FakeForecastRepository()
        val useCase = GetForecastsUseCase(repository)

        fun createForecast(utilityType: UtilityType): ForecastedConsumption {
            val tomorrow =
                Clock.System
                    .now()
                    .toLocalDateTime(TimeZone.UTC)
                    .date
                    .plus(1, DateTimeUnit.DAY)

            return ForecastedConsumption.create(
                utilityType,
                listOf(ForecastedDataPoint(tomorrow, ConsumptionValue.of(100.0))),
            )
        }

        afterContainer {
            repository.clear()
        }

        Given("an empty forecast repository") {
            When("retrieving all forecasts") {
                Then("it should return an empty list") {
                    useCase.getAll() shouldHaveSize 0
                }
            }

            When("retrieving a forecast by utility type") {
                Then("it should return null") {
                    useCase.getByUtility(UtilityType.GAS) shouldBe null
                }
            }
        }

        Given("a single gas forecast in the repository") {
            val gas = createForecast(UtilityType.GAS)
            repository.save(gas)

            When("retrieving all forecasts") {
                val forecasts = useCase.getAll()

                Then("it should return a list with that forecast") {
                    forecasts shouldHaveSize 1
                    forecasts.first() shouldBe gas
                }
            }
        }

        Given("multiple forecasts in the repository") {
            val electricity = createForecast(UtilityType.ELECTRICITY)
            val water = createForecast(UtilityType.WATER)
            val gas = createForecast(UtilityType.GAS)
            repository.save(electricity)
            repository.save(water)
            repository.save(gas)

            When("retrieving all forecasts") {
                val forecasts = useCase.getAll()

                Then("it should return all available forecasts") {
                    forecasts shouldHaveSize 3
                    forecasts.map { it.utilityType }.toSet() shouldBe
                        setOf(UtilityType.ELECTRICITY, UtilityType.WATER, UtilityType.GAS)
                }
            }
        }

        Given("an electricity forecast in the repository") {
            val forecast = createForecast(UtilityType.ELECTRICITY)
            repository.save(forecast)

            When("retrieving by electricity type") {
                val result = useCase.getByUtility(UtilityType.ELECTRICITY)

                Then("it should return the forecast") {
                    result shouldBe forecast
                }
            }
        }
    })
