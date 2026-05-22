package io.energyconsumptionoptimizer.forecastservice.domain.entity

import io.energyconsumptionoptimizer.forecastservice.domain.event.ForecastComputedEvent
import io.energyconsumptionoptimizer.forecastservice.domain.value.ForecastedDataSeries
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import kotlin.time.Clock
import kotlin.time.Instant

class Forecast private constructor(
    val utilityType: UtilityType,
    series: ForecastedDataSeries,
    computedAt: Instant,
) : AggregateRoot() {
    var series: ForecastedDataSeries = series
        private set

    var computedAt: Instant = computedAt
        private set

    fun recompute(
        newSeries: ForecastedDataSeries,
        at: Instant = Clock.System.now(),
    ) {
        series = newSeries
        computedAt = at

        addDomainEvent(
            ForecastComputedEvent(
                utilityType = utilityType,
                dataPoints = newSeries,
                occurredAt = at,
            ),
        )
    }

    override fun equals(other: Any?): Boolean = other is Forecast && utilityType == other.utilityType

    override fun hashCode(): Int = utilityType.hashCode()

    companion object {
        fun create(
            utilityType: UtilityType,
            newSeries: ForecastedDataSeries,
            at: Instant = Clock.System.now(),
        ): Forecast {
            val forecast = Forecast(utilityType, newSeries, at)

            forecast.addDomainEvent(
                ForecastComputedEvent(
                    utilityType = utilityType,
                    dataPoints = newSeries,
                    occurredAt = at,
                ),
            )
            return forecast
        }

        fun restore(
            utilityType: UtilityType,
            dataPoints: ForecastedDataSeries,
            at: Instant,
        ): Forecast = Forecast(utilityType, dataPoints, at)
    }
}
