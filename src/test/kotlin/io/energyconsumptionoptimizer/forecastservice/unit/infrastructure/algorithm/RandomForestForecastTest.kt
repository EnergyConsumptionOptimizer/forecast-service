package io.energyconsumptionoptimizer.forecastservice.unit.infrastructure.algorithm

import arrow.core.raise.either
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.historicalData
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.infrastructure.algorithm.RandomForestForecast
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.beGreaterThanOrEqualTo
import io.kotest.matchers.doubles.beGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class RandomForestForecastTest :
    DescribeSpec({

        val validHistoricalSeries = historicalData(30)
        val horizon = 5

        describe("RandomForestForecast") {

            describe("forecast") {

                context("given sufficient historical data") {
                    val algorithm = RandomForestForecast()

                    it("should return an AlgorithmPrediction with the expected number of forecasted data points") {
                        val result = either { algorithm.forecast(validHistoricalSeries, horizon) }.shouldBeRight()
                        result.predictions shouldHaveSize horizon
                    }

                    it("should produce predictions with dates after the last historical date") {
                        val lastHistoricalDate = validHistoricalSeries.points.last().date
                        val result = either { algorithm.forecast(validHistoricalSeries, horizon) }.shouldBeRight()
                        result.predictions.forEach { point ->
                            point.date shouldBe beGreaterThanOrEqualTo(lastHistoricalDate)
                        }
                    }

                    it("should produce non-zero predictions") {
                        val result = either { algorithm.forecast(validHistoricalSeries, horizon) }.shouldBeRight()
                        result.predictions.forEach { point ->
                            point.value shouldBe beGreaterThan(0.0)
                        }
                    }
                }

                context("given insufficient historical data") {
                    it("should raise AlgorithmError") {
                        val algorithm = RandomForestForecast()
                        val insufficientData = historicalData(3)

                        val error = either { algorithm.forecast(insufficientData, horizon) }.shouldBeLeft()

                        error.shouldBeInstanceOf<ApplicationError.AlgorithmError>()
                    }
                }
            }
        }
    })
