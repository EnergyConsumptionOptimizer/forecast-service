package io.energyconsumptionoptimizer.forecastservice.presentation.dto

import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.serialization.Serializable
import kotlin.time.Instant
import io.energyconsumptionoptimizer.forecastservice.application.ports.dto.ForecastResponse as AppForecastResponse

@Serializable
data class ForecastResponse(
    val id: String,
    val utilityType: UtilityType,
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
)

fun AppForecastResponse.toPresentationDTO(): ForecastResponse {
    val first = dataPoints.first().date
    val last = dataPoints.last().date
    return ForecastResponse(
        id = utilityType.name,
        utilityType = utilityType,
        dataPoints = dataPoints.map { DataPointResponse(it.date, it.value) },
        computedAt = computedAt,
        startDate = first,
        endDate = last,
        durationDays = first.daysUntil(last).toLong() + 1,
    )
}
