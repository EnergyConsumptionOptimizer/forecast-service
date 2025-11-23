package io.energyconsumptionoptimizer.forecastingservice.utils.fakes

import io.energyconsumptionoptimizer.forecastingservice.domain.port.ThresholdNotifier
import io.energyconsumptionoptimizer.forecastingservice.domain.value.PeriodType
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType

class FakeThresholdNotifier : ThresholdNotifier {
    val notifications = mutableListOf<Pair<UtilityType, Map<PeriodType, Double>>>()

    override suspend fun notifyForecastAggregations(
        utilityType: UtilityType,
        aggregations: Map<PeriodType, Double>,
    ) {
        notifications.add(utilityType to aggregations)
    }
}
