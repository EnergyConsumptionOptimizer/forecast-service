package io.energyconsumptionoptimizer.forecastservice.infrastructure.persistence.mongo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForecastDocument(
    @SerialName("_id")
    val utilityType: String,
    val dataPoints: List<DataPointDocument>,
    val computedAt: Long,
)

@Serializable
data class DataPointDocument(
    val date: kotlinx.datetime.LocalDate,
    val value: Double,
)
