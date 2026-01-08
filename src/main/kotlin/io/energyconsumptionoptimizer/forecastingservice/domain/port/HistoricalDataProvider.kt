package io.energyconsumptionoptimizer.forecastingservice.domain.port

import io.energyconsumptionoptimizer.forecastingservice.domain.value.ConsumptionValue
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType
import kotlinx.datetime.LocalDate

/**
 * A single historical observation used as input to forecasting algorithms.
 *
 * @property timestamp date of the observation
 * @property value consumption value for the date
 */
data class HistoricalData(
    val timestamp: LocalDate,
    val value: ConsumptionValue,
)

/**
 * Port responsible for retrieving aggregated historical data for a given utility.
 */
interface HistoricalDataProvider {
    /**
     * Fetch aggregated historical observations for `utilityType`.
     *
     * Implementations should return data ordered by ascending `timestamp`.
     *
     * @param utilityType Utility for which to fetch historical observations.
     * @return Ordered list of [HistoricalData] points (may be empty when no history exists).
     */
    suspend fun fetchAggregatedHistoricalData(utilityType: UtilityType): List<HistoricalData>
}
