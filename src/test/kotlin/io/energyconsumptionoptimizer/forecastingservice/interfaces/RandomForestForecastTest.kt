package io.energyconsumptionoptimizer.forecastingservice.interfaces

import io.energyconsumptionoptimizer.forecastingservice.domain.port.HistoricalData
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ConsumptionValue
import io.energyconsumptionoptimizer.forecastingservice.interfaces.algorithm.RandomForestForecast
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class RandomForestForecastTest :
    FunSpec({

        val algorithm = RandomForestForecast(windowSize = 5, nTrees = 10)

        fun generateHistoricalData(
            start: Int,
            end: Int,
            increment: Double = 1.0,
        ) = (start..end).map {
            HistoricalData(
                timestamp = LocalDate(2025, 11, it),
                value = ConsumptionValue.of(100.0 + it * increment),
            )
        }

        context("Forecast generation") {

            test("produces forecast with correct horizon and dates") {
                val historicalData = generateHistoricalData(1, 20)
                val horizon = 5

                val result = algorithm.forecast(historicalData, horizon)

                result shouldHaveSize horizon
                result.first().date shouldBe LocalDate(2025, 11, 21)
                result.last().date shouldBe LocalDate(2025, 11, 25)
            }

            test("predicts reasonable positive values for increasing trend") {
                val historicalData = generateHistoricalData(1, 20, increment = 2.0)
                val horizon = 3

                val result = algorithm.forecast(historicalData, horizon)

                result.forEach {
                    it.predictedValue.amount shouldBeGreaterThan 0.0
                }
            }
        }

        context("Algorithm metadata") {

            test("returns correct algorithm name") {
                algorithm.name shouldBe "RandomForest"
            }
        }
    })
