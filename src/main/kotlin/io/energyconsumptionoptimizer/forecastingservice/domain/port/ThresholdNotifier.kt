package io.energyconsumptionoptimizer.forecastingservice.domain.port

import io.energyconsumptionoptimizer.forecastingservice.domain.value.PeriodType
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType

interface ThresholdNotifier {
    suspend fun notifyForecastAggregations(
        utilityType: UtilityType,
        aggregations: Map<PeriodType, Double>,
    )
}
