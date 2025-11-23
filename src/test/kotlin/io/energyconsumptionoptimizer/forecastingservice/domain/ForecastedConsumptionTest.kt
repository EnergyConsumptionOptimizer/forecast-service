package io.energyconsumptionoptimizer.forecastingservice.domain

import io.energyconsumptionoptimizer.forecastingservice.domain.value.ConsumptionValue
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastedDataPoint
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class ForecastedConsumptionTest :
    BehaviorSpec({
        val tomorrow = LocalDate.now().plusDays(1)

        Given("a forecasted consumption creation") {
            When("attempting to create with empty data points") {
                Then("it should reject with an error") {
                    shouldThrow<IllegalArgumentException> {
                        ForecastedConsumption.create(
                            utilityType = UtilityType.ELECTRICITY,
                            forecastedDataPoints = emptyList(),
                        )
                    }.message shouldBe "Forecast must contain at least one daily prediction"
                }
            }

            When("creating with unordered data points") {
                val forecast =
                    ForecastedConsumption.create(
                        utilityType = UtilityType.WATER,
                        forecastedDataPoints =
                            listOf(
                                ForecastedDataPoint(tomorrow.plusDays(3), ConsumptionValue.of(40.0)),
                                ForecastedDataPoint(tomorrow.plusDays(1), ConsumptionValue.of(30.0)),
                                ForecastedDataPoint(tomorrow, ConsumptionValue.of(20.0)),
                            ),
                    )

                Then("it should sort data points chronologically") {
                    forecast.forecastedDataPoints.map { it.date } shouldBe
                        listOf(tomorrow, tomorrow.plusDays(1), tomorrow.plusDays(3))
                }
            }
        }

        Given("a forecast with multiple data points") {
            val forecast =
                ForecastedConsumption.create(
                    utilityType = UtilityType.GAS,
                    forecastedDataPoints =
                        listOf(
                            ForecastedDataPoint(tomorrow, ConsumptionValue.of(10.0)),
                            ForecastedDataPoint(tomorrow.plusDays(2), ConsumptionValue.of(20.0)),
                        ),
                )

            When("computing the duration") {
                Then("it should calculate the correct number of days") {
                    forecast.durationDays shouldBe 3
                }
            }
        }

        Given("a forecast covering five days") {
            val forecast =
                ForecastedConsumption.create(
                    utilityType = UtilityType.ELECTRICITY,
                    forecastedDataPoints =
                        (1..5).map {
                            ForecastedDataPoint(tomorrow.plusDays(it.toLong()), ConsumptionValue.of(it * 10.0))
                        },
                )

            When("filtering forecasts within a valid date range") {
                val filtered =
                    forecast.forecastsInRange(
                        tomorrow.plusDays(2),
                        tomorrow.plusDays(4),
                    )

                Then("it should return only forecasts within that range") {
                    filtered shouldHaveSize 3
                    filtered.first().date shouldBe tomorrow.plusDays(2)
                    filtered.last().date shouldBe tomorrow.plusDays(4)
                }
            }

            When("attempting to filter with an invalid date range") {
                Then("it should reject with an error") {
                    shouldThrow<IllegalArgumentException> {
                        forecast.forecastsInRange(tomorrow.plusDays(2), tomorrow)
                    }.message shouldBe "End date cannot be before start date"
                }
            }
        }
    })
