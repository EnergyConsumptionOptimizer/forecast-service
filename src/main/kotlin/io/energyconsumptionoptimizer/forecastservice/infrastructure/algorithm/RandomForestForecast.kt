package io.energyconsumptionoptimizer.forecastservice.infrastructure.algorithm

import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.context.raise
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.application.ports.dto.AlgorithmPrediction
import io.energyconsumptionoptimizer.forecastservice.application.ports.dto.DataPoint
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.ForecastAlgorithm
import io.energyconsumptionoptimizer.forecastservice.domain.value.HistoricalDataSeries
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import smile.data.DataFrame
import smile.data.formula.Formula
import smile.regression.RandomForest

class RandomForestForecast(
    private val windowSize: Int = 7,
    private val nTrees: Int = 100,
    private val maxDepth: Int = 20,
    private val nodeSize: Int = 5,
) : ForecastAlgorithm {
    private val featureColumnNames = Array(windowSize) { "x$it" }
    private val trainingColumnNames = featureColumnNames + "target"

    context(raise: Raise<ApplicationError.AlgorithmError>)
    override suspend fun forecast(
        historicalData: HistoricalDataSeries,
        horizon: Int,
    ): AlgorithmPrediction =
        catch(
            {
                val consumptionValues = historicalData.points.map { it.value.amount }
                val lastHistoricalDate = historicalData.points.last().date

                val trainingData = createTrainingDataFrame(consumptionValues)
                val model = trainModel(trainingData)
                val predictions = generatePredictions(model, consumptionValues, horizon)

                val points =
                    predictions.mapIndexed { dayIndex, predictedValue ->
                        val date = lastHistoricalDate.plus(dayIndex + 1, DateTimeUnit.DAY)
                        DataPoint(date, predictedValue)
                    }

                AlgorithmPrediction(points)
            },
            { e: Exception ->
                raise(ApplicationError.AlgorithmError("RandomForest Smile: ${e.message}"))
            },
        )

    private fun createTrainingDataFrame(values: List<Double>): DataFrame {
        val numSamples = values.size - windowSize
        val rows =
            Array(numSamples) { rowIndex ->
                DoubleArray(windowSize + 1) { columnIndex ->
                    when {
                        columnIndex < windowSize -> values[rowIndex + columnIndex]
                        else -> values[rowIndex + windowSize]
                    }
                }
            }

        @Suppress("SpreadOperator")
        return DataFrame.of(rows, *trainingColumnNames)
    }

    private fun trainModel(data: DataFrame): RandomForest {
        val formula = Formula.lhs("target")
        val mtry = calculateMtry()

        val options =
            RandomForest.Options(
                nTrees,
                mtry,
                maxDepth,
                Int.MAX_VALUE,
                nodeSize,
                1.0,
                null,
                null,
            )

        return RandomForest.fit(formula, data, options)
    }

    private fun calculateMtry(): Int = (windowSize / 3).coerceAtLeast(1)

    private fun generatePredictions(
        model: RandomForest,
        historicalValues: List<Double>,
        horizon: Int,
    ): List<Double> {
        val predictions = mutableListOf<Double>()
        val slidingWindow = historicalValues.takeLast(windowSize).toMutableList()

        repeat(horizon) {
            val featureRow = arrayOf(slidingWindow.toDoubleArray())

            @Suppress("SpreadOperator")
            val inputData = DataFrame.of(featureRow, *featureColumnNames)

            val nextValue = model.predict(inputData)[0]
            predictions.add(nextValue)

            slidingWindow.removeFirst()
            slidingWindow.add(nextValue)
        }

        return predictions
    }
}
