package io.energyconsumptionoptimizer.forecastingservice.application.usecases

import io.energyconsumptionoptimizer.forecastingservice.domain.ForecastedConsumption
import io.energyconsumptionoptimizer.forecastingservice.domain.port.ForecastRepository
import io.energyconsumptionoptimizer.forecastingservice.domain.port.ForecastingAlgorithm
import io.energyconsumptionoptimizer.forecastingservice.domain.port.HistoricalDataProvider
import io.energyconsumptionoptimizer.forecastingservice.domain.port.ThresholdNotifier
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastedDataPoint
import io.energyconsumptionoptimizer.forecastingservice.domain.value.PeriodType
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

/**
 * Computes forecasts for utilities and persists the results.
 *
 * Coordinates retrieval of historical observations via [HistoricalDataProvider], delegates
 * forecasting logic to a [ForecastingAlgorithm], aggregates predicted values per
 * period, notifies thresholds through [ThresholdNotifier], and persists the resulting
 * [ForecastedConsumption] via [ForecastRepository]. Methods prefer non-blocking
 * concurrency primitives; `computeAll` runs per-utility computations concurrently.
 *
 * @param repository Repository used to persist computed [ForecastedConsumption].
 * @param historicalDataProvider Source port for aggregated historical observations.
 * @param forecastingAlgorithm Algorithm used to produce time-series forecasts.
 * @param thresholdNotifier Notifies external systems when aggregated forecasts exceed thresholds.
 * @constructor Creates a use case instance with the required domain ports and collaborators.
 */
class ComputeForecastUseCase(
    private val repository: ForecastRepository,
    private val historicalDataProvider: HistoricalDataProvider,
    private val forecastingAlgorithm: ForecastingAlgorithm,
    private val thresholdNotifier: ThresholdNotifier,
) {
    /**
     * Computes forecasts for all supported utilities concurrently.
     *
     * Launches one coroutine per entry in [UtilityType.entries], invoking [compute]
     * and collecting successful results. Failures for individual utilities are
     * isolated so one failing utility does not cancel the entire operation.
     *
     * @return A `List` of successfully computed [ForecastedConsumption] objects.
     */
    suspend fun computeAll(): List<ForecastedConsumption> =
        coroutineScope {
            UtilityType.entries
                .map { async { runCatching { compute(it) }.getOrNull() } }
                .awaitAll()
                .filterNotNull()
        }

    /**
     * Computes a forecast for a single [utilityType].
     *
     * Steps performed:
     * 1. Fetches aggregated historical observations from [HistoricalDataProvider].
     * 2. Determines an optimal forecast horizon based on available history.
     * 3. Delegates prediction to [ForecastingAlgorithm].
     * 4. Notifies aggregations to [ThresholdNotifier].
     * 5. Persists the created [ForecastedConsumption] using [ForecastRepository].
     *
     * @param utilityType Type of utility to compute the forecast for, see [UtilityType].
     * @return The persisted [ForecastedConsumption] for the requested utility.
     * @throws IllegalArgumentException If no historical data points are available to compute a horizon.
     */
    suspend fun compute(utilityType: UtilityType): ForecastedConsumption {
        val historicalData = historicalDataProvider.fetchAggregatedHistoricalData(utilityType)
        val forecastHorizon = calculateOptimalHorizon(historicalData.size)
        val dataPoints = forecastingAlgorithm.forecast(historicalData, forecastHorizon)

        thresholdNotifier.notifyForecastAggregations(utilityType, calculateAggregations(dataPoints))

        return ForecastedConsumption
            .create(utilityType, dataPoints)
            .also { repository.save(it) }
    }

    private fun calculateOptimalHorizon(historicalDataPoints: Int): Int {
        require(historicalDataPoints > 0) { "Historical data points must be more than 0, got: $historicalDataPoints" }
        return (historicalDataPoints * 0.3).toInt().coerceAtLeast(1)
    }

    private fun calculateAggregations(dataPoints: List<ForecastedDataPoint>): Map<PeriodType, Double> =
        if (dataPoints.isEmpty()) {
            emptyMap()
        } else {
            val startDate = dataPoints.first().date
            PeriodType.entries.associateWith { period ->
                val endDate = startDate.plus(period.days - 1, DateTimeUnit.DAY)
                dataPoints
                    .takeWhile { it.date <= endDate }
                    .sumOf { it.predictedValue.amount }
            }
        }
}
