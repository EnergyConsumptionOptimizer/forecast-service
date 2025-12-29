package io.energyconsumptionoptimizer.forecastingservice.storage.mongodb.document

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ForecastedConsumptionDocument(
    @Suppress("ConstructorParameterNaming")
    val _id: String,
    val utilityType: String,
    val dataPoints: List<ForecastedDataPointDocument>,
    val computedAt: Instant,
)

@Serializable
data class ForecastedDataPointDocument(
    val date: LocalDate,
    val predictedValue: Double,
)
