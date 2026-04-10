package io.energyconsumptionoptimizer.forecastservice.utils.fakes

import io.energyconsumptionoptimizer.forecastservice.domain.port.ThresholdNotifier
import io.energyconsumptionoptimizer.forecastservice.domain.value.PeriodType
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType

class FakeThresholdNotifier : ThresholdNotifier {
    val notifications = mutableListOf<Pair<UtilityType, Map<PeriodType, Double>>>()

    override suspend fun notifyForecastAggregations(
        utilityType: UtilityType,
        aggregations: Map<PeriodType, Double>,
    ) {
        notifications.add(utilityType to aggregations)
    }
}
