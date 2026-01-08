package io.energyconsumptionoptimizer.forecastingservice.domain.port

import io.energyconsumptionoptimizer.forecastingservice.domain.value.PeriodType
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType

/**
 * Port for notifying external systems about aggregated forecast values.
 *
 * Implementations are responsible for delivering aggregation summaries
 * (for example: daily/week/month totals) to interested parties.
 */
interface ThresholdNotifier {
    /**
     * Notify about computed aggregations for a specific `utilityType`.
     *
     * Implementations are free to deliver the data asynchronously; the use-case
     * awaits this call to ensure notifications are scheduled or delivered.
     *
     * @param utilityType Type of utility the aggregations belong to.
     * @param aggregations Mapping from [PeriodType] to aggregated consumption value.
     */
    suspend fun notifyForecastAggregations(
        utilityType: UtilityType,
        aggregations: Map<PeriodType, Double>,
    )
}
