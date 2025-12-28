package io.energyconsumptionoptimizer.forecastingservice.domain.port

import io.energyconsumptionoptimizer.forecastingservice.domain.value.ConsumptionValue
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType
import kotlinx.datetime.LocalDate

data class HistoricalData(
    val timestamp: LocalDate,
    val value: ConsumptionValue,
)

interface HistoricalDataProvider {
    suspend fun fetchAggregatedHistoricalData(utilityType: UtilityType): List<HistoricalData>
}
