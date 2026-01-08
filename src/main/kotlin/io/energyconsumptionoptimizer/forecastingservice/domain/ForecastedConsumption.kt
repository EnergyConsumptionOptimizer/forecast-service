package io.energyconsumptionoptimizer.forecastingservice.domain

import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastId
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastedDataPoint
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Aggregated forecast entity consisting of ordered daily predictions for a utility.
 *
 * Instances are immutable and validated on creation; use `create` for new forecasts
 * and `fromPersistence` when loading from storage.
 *
 * @property id unique forecast identifier
 * @property utilityType type of utility this forecast targets
 * @property forecastedDataPoints non-empty ordered list of daily predictions
 * @property computedAt timestamp when the forecast was computed
 */
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

    /**
     * First date included in the forecast range.
     */
    val startDate: LocalDate get() = forecastedDataPoints.first().date

    /**
     * Last date included in the forecast range.
     */
    val endDate: LocalDate get() = forecastedDataPoints.last().date

    /**
     * Duration of the forecast in days (inclusive).
     */
    val durationDays: Long get() = (startDate.daysUntil(endDate) + 1).toLong()

    /**
     * Return predictions that fall within the inclusive date range [start, end].
     *
     * Validates that `start <= end`.
     *
     * @param start Start of the requested range (inclusive).
     * @param end End of the requested range (inclusive).
     * @return Ordered list of [ForecastedDataPoint] that fall inside the range.
     */
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
        /**
         * Create a new forecast instance with a generated id.
         *
         * @param utilityType The `UtilityType` this forecast targets.
         * @param forecastedDataPoints Non-empty ordered list of daily predictions.
         * @param computedAt Timestamp when the forecast was computed. Defaults to now.
         * @return Newly created persisted-ready [ForecastedConsumption] with a generated [ForecastId].
         */
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

        /**
         * Reconstruct a forecast instance from persisted values.
         *
         * @param id Persisted [ForecastId].
         * @param utilityType The `UtilityType` this forecast targets.
         * @param forecastedDataPoints Non-empty ordered list of daily predictions.
         * @param computedAt Timestamp when the forecast was computed.
         * @return Reconstructed [ForecastedConsumption] preserving the provided id.
         */
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
