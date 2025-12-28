package io.energyconsumptionoptimizer.forecastingservice.interfaces.algorithm

import io.energyconsumptionoptimizer.forecastingservice.domain.port.ForecastingAlgorithm
import io.energyconsumptionoptimizer.forecastingservice.domain.port.HistoricalData
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ConsumptionValue
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastedDataPoint
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import smile.data.DataFrame
import smile.data.formula.Formula
import smile.regression.RandomForest

/**
 * Random Forest forecasting algorithm using sliding window approach.
 *
 * Trains a Random Forest regression model using historical time series data
 * with a sliding window technique, then generates future predictions iteratively.
 *
 * @property windowSize Number of previous time steps used as features (default: 7).
 *                      Larger values capture more temporal patterns but require more data.
 * @property nTrees Number of decision trees in the forest (default: 100).
 *                  More trees generally improve accuracy but increase computation time.
 * @property maxDepth Maximum depth of each decision tree (default: 20).
 *                    Controls tree complexity and helps prevent overfitting.
 * @property nodeSize Minimum number of samples required in a leaf node (default: 5).
 *                    Larger values create more generalized trees, reducing overfitting.
 */
class RandomForestForecast(
    private val windowSize: Int = 7,
    private val nTrees: Int = 100,
    private val maxDepth: Int = 20,
    private val nodeSize: Int = 5,
) : ForecastingAlgorithm {
    override val name: String = "RandomForest"

    // Pre-computed column names to avoid repeated array allocations
    private val featureColumnNames = Array(windowSize) { "x$it" }
    private val trainingColumnNames = featureColumnNames + "target"

    override suspend fun forecast(
        historicalData: List<HistoricalData>,
        horizon: Int,
    ): List<ForecastedDataPoint> {
        validateInputs(historicalData, horizon)

        val consumptionValues = historicalData.map { it.value.amount }
        val lastHistoricalDate = historicalData.last().timestamp

        val trainingData = createTrainingDataFrame(consumptionValues)
        val model = trainModel(trainingData)
        val predictions = generatePredictions(model, consumptionValues, horizon)

        // Convert predictions to domain objects
        return predictions.mapIndexed { dayIndex, predictedValue ->
            ForecastedDataPoint(
                date = lastHistoricalDate.plus(dayIndex + 1, DateTimeUnit.DAY),
                predictedValue = ConsumptionValue.of(predictedValue),
            )
        }
    }

    /**
     * Creates a DataFrame for training using sliding window approach.
     *
     * Each row contains [windowSize] features (x0, x1, ..., x[windowSize-1])
     * representing consecutive historical values, plus the target value.
     *
     * Example with windowSize=3:
     * - Row 0: [values[0], values[1], values[2], target=values[3]]
     * - Row 1: [values[1], values[2], values[3], target=values[4]]
     * - ...
     */
    private fun createTrainingDataFrame(values: List<Double>): DataFrame {
        val numSamples = values.size - windowSize
        val rows =
            Array(numSamples) { rowIndex ->
                DoubleArray(windowSize + 1) { columnIndex ->
                    when {
                        columnIndex < windowSize -> values[rowIndex + columnIndex]

                        // Feature columns
                        else -> values[rowIndex + windowSize] // Target column
                    }
                }
            }

        @Suppress("SpreadOperator") // Unavoidable: Smile DataFrame.of() requires vararg String
        return DataFrame.of(rows, *trainingColumnNames)
    }

    /**
     * Trains a Random Forest regression model.
     *
     * Configures hyperparameters according to best practices:
     * - mtry: Number of random features to consider at each split (windowSize/3)
     * - subsample: 1.0 for bootstrap sampling (sampling with replacement)
     */
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

    /**
     * Calculates the number of random features (mtry) to consider at each split.
     *
     * Following Random Forest best practice: p/3 for regression problems,
     * where p is the number of input features.
     */
    private fun calculateMtry(): Int = (windowSize / 3).coerceAtLeast(1)

    /**
     * Generates future predictions iteratively using a sliding window.
     *
     * For each prediction:
     * 1. Use the last [windowSize] values as features
     * 2. Predict the next value
     * 3. Slide the window forward by adding the prediction
     * 4. Repeat for [horizon] steps
     *
     * This creates a multi-step ahead forecast where each prediction
     * depends on previous predictions.
     */
    private fun generatePredictions(
        model: RandomForest,
        historicalValues: List<Double>,
        horizon: Int,
    ): List<Double> {
        val predictions = mutableListOf<Double>()
        val slidingWindow = historicalValues.takeLast(windowSize).toMutableList()

        repeat(horizon) {
            // Create DataFrame with current window as features
            val featureRow = arrayOf(slidingWindow.toDoubleArray())

            @Suppress("SpreadOperator") // Unavoidable: Smile DataFrame.of() requires vararg String
            val inputData = DataFrame.of(featureRow, *featureColumnNames)

            // Predict next value
            val nextValue = model.predict(inputData)[0]
            predictions.add(nextValue)

            // Slide window forward
            slidingWindow.removeFirst()
            slidingWindow.add(nextValue)
        }

        return predictions
    }
}
