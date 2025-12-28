package io.energyconsumptionoptimizer.forecastingservice.presentation.dto

import io.energyconsumptionoptimizer.forecastingservice.domain.ForecastedConsumption
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastedDataPoint
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Instant

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

@Serializable
data class DataPointResponse(
    val date: LocalDate,
    val predictedConsumption: Double,
)

@Serializable
data class ForecastListResponse(
    val forecasts: List<ForecastResponse>,
) {
    val count: Int get() = forecasts.size
}

@Serializable
enum class UtilityTypeDto {
    ELECTRICITY,
    GAS,
    WATER,
}

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

fun ForecastedDataPoint.toDTO(): DataPointResponse =
    DataPointResponse(
        date = date,
        predictedConsumption = predictedValue.amount,
    )

fun List<ForecastedConsumption>.toListDTO(): ForecastListResponse = ForecastListResponse(map { it.toDTO() })
