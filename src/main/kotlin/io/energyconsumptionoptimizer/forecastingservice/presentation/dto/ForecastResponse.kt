package io.energyconsumptionoptimizer.forecastingservice.presentation.dto

import io.energyconsumptionoptimizer.forecastingservice.domain.ForecastedConsumption
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastedDataPoint
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * API response representing a single forecast.
 *
 * @property id Forecast identifier as a string.
 * @property utilityType The reported utility type.
 * @property dataPoints Ordered list of daily predictions.
 * @property computedAt Timestamp when the forecast was computed.
 * @property startDate First date included in the forecast range.
 * @property endDate Last date included in the forecast range.
 * @property durationDays Duration of the forecast in days (inclusive).
 */
@Serializable
data class ForecastResponse(
    val id: String,
    val utilityType: UtilityTypeDto,
    val dataPoints: List<DataPointResponse>,
    val computedAt: Instant,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val durationDays: Long,
)

/**
 * API representation of a single daily prediction.
 *
 * @property date The day the prediction refers to.
 * @property predictedConsumption Raw predicted consumption value for the day.
 */
@Serializable
data class DataPointResponse(
    val date: LocalDate,
    val predictedConsumption: Double,
)

/**
 * Container response for lists of forecasts.
 *
 * @property forecasts List of [ForecastResponse] items.
 */
@Serializable
data class ForecastListResponse(
    val forecasts: List<ForecastResponse>,
) {
    /** Number of forecasts included in the response. */
    val count: Int get() = forecasts.size
}

/** Supported utility types exposed by the HTTP API. */
@Serializable
enum class UtilityTypeDto {
    ELECTRICITY,
    GAS,
    WATER,
}

/**
 * Convert a domain [ForecastedConsumption] into an API [ForecastResponse].
 *
 * @receiver Domain forecast to convert.
 * @return Serialized [ForecastResponse] suitable for HTTP responses.
 */
fun ForecastedConsumption.toDTO(): ForecastResponse =
    ForecastResponse(
        id = id.value.toString(),
        utilityType = UtilityTypeDto.valueOf(utilityType.name),
        dataPoints = forecastedDataPoints.map { it.toDTO() },
        computedAt = computedAt,
        startDate = startDate,
        endDate = endDate,
        durationDays = durationDays,
    )

/**
 * Convert a domain [ForecastedDataPoint] into a [DataPointResponse].
 *
 * @receiver Domain data point to convert.
 * @return API-friendly [DataPointResponse].
 */
fun ForecastedDataPoint.toDTO(): DataPointResponse =
    DataPointResponse(
        date = date,
        predictedConsumption = predictedValue.amount,
    )

/**
 * Convert a list of domain forecasts to a [ForecastListResponse].
 *
 * @receiver List of domain [ForecastedConsumption].
 * @return [ForecastListResponse] wrapping the converted items.
 */
fun List<ForecastedConsumption>.toListDTO(): ForecastListResponse = ForecastListResponse(map { it.toDTO() })
