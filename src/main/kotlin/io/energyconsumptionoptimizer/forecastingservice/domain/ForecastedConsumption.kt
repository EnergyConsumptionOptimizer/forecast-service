package io.energyconsumptionoptimizer.forecastingservice.domain

import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastId
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastedDataPoint
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlin.time.Clock
import kotlin.time.Instant

class ForecastedConsumption private constructor(
    val id: ForecastId,
    val utilityType: UtilityType,
    forecastedDataPoints: List<ForecastedDataPoint>,
    val computedAt: Instant,
) {
    val forecastedDataPoints: List<ForecastedDataPoint> = forecastedDataPoints.sortedBy { it.date }

    init {
        require(this.forecastedDataPoints.isNotEmpty()) { "Forecast must contain at least one daily prediction" }
    }

    val startDate: LocalDate get() = forecastedDataPoints.first().date
    val endDate: LocalDate get() = forecastedDataPoints.last().date
    val durationDays: Long get() = (startDate.daysUntil(endDate) + 1).toLong()

    fun forecastsInRange(
        start: LocalDate,
        end: LocalDate,
    ): List<ForecastedDataPoint> {
        require(start <= end) { "End date cannot be before start date" }
        return forecastedDataPoints.filter { it.date in start..end }
    }

    override fun equals(other: Any?): Boolean = this === other || (other is ForecastedConsumption && id == other.id)

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String =
        "ForecastedConsumption(id=$id, utilityType=$utilityType, " +
            "points=${forecastedDataPoints.size}, computedAt=$computedAt)"

    companion object {
        fun create(
            utilityType: UtilityType,
            forecastedDataPoints: List<ForecastedDataPoint>,
            computedAt: Instant = Clock.System.now(),
        ): ForecastedConsumption =
            ForecastedConsumption(
                id = ForecastId.generate(),
                utilityType = utilityType,
                forecastedDataPoints = forecastedDataPoints,
                computedAt = computedAt,
            )

        fun fromPersistence(
            id: ForecastId,
            utilityType: UtilityType,
            forecastedDataPoints: List<ForecastedDataPoint>,
            computedAt: Instant,
        ): ForecastedConsumption =
            ForecastedConsumption(
                id = id,
                utilityType = utilityType,
                forecastedDataPoints = forecastedDataPoints,
                computedAt = computedAt,
            )
    }
}
