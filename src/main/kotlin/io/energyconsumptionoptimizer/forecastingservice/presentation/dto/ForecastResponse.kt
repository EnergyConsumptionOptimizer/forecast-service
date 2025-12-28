package io.energyconsumptionoptimizer.forecastingservice.presentation.dto

import io.energyconsumptionoptimizer.forecastingservice.domain.ForecastedConsumption
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastedDataPoint
import io.energyconsumptionoptimizer.forecastingservice.presentation.serializers.InstantSerializer
import io.energyconsumptionoptimizer.forecastingservice.presentation.serializers.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate

@Serializable
data class ForecastResponse(
    val id: String,
    val utilityType: UtilityTypeDto,
    val dataPoints: List<DataPointResponse>,
    @Serializable(with = InstantSerializer::class)
    val computedAt: Instant,
    @Serializable(with = LocalDateSerializer::class)
    val startDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val endDate: LocalDate,
    val durationDays: Long,
)

@Serializable
data class DataPointResponse(
    @Serializable(with = LocalDateSerializer::class)
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
