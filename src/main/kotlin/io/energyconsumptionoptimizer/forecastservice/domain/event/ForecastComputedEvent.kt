package io.energyconsumptionoptimizer.forecastservice.domain.event

import io.energyconsumptionoptimizer.forecastservice.domain.value.ForecastedDataSeries
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import kotlin.time.Clock
import kotlin.time.Instant

data class ForecastComputedEvent(
    val utilityType: UtilityType,
    val dataPoints: ForecastedDataSeries,
    override val occurredAt: Instant = Clock.System.now(),
) : DomainEvent {
    override val eventType = "ForecastComputedEvent"
    override val aggregateType = "Forecast"
    override val aggregateId: String = utilityType.name
}
