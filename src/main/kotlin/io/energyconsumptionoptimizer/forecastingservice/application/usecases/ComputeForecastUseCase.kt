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

class ComputeForecastUseCase(
    private val repository: ForecastRepository,
    private val historicalDataProvider: HistoricalDataProvider,
    private val forecastingAlgorithm: ForecastingAlgorithm,
    private val thresholdNotifier: ThresholdNotifier,
) {
    suspend fun computeAll(): List<ForecastedConsumption> =
        coroutineScope {
            UtilityType.entries
                .map { async { runCatching { compute(it) }.getOrNull() } }
                .awaitAll()
                .filterNotNull()
        }

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
