package io.energyconsumptionoptimizer.forecastingservice.domain

import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastId
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastedDataPoint
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
    val durationDays: Long get() = ChronoUnit.DAYS.between(startDate, endDate) + 1

    fun forecastsInRange(
        start: LocalDate,
        end: LocalDate,
    ): List<ForecastedDataPoint> {
        require(!end.isBefore(start)) { "End date cannot be before start date" }
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
            computedAt: Instant = Instant.now(),
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
