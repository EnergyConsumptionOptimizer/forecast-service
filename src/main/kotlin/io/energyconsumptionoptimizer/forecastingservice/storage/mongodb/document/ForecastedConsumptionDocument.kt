package io.energyconsumptionoptimizer.forecastingservice.storage.mongodb.document

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class ForecastedConsumptionDocument(
    @Suppress("ConstructorParameterNaming")
    val _id: String,
    val utilityType: String,
    val dataPoints: List<ForecastedDataPointDocument>,
    val computedAt: Instant,
)

data class ForecastedDataPointDocument(
    val date: LocalDate,
    val predictedValue: Double,
)
