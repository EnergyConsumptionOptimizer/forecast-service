package io.energyconsumptionoptimizer.forecastingservice.storage.mongodb.document

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Persistence representation of a forecast stored in MongoDB.
 *
 * Fields are intentionally simple types to ease serialization.
 *
 * @property _id Stringified [io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastId] used as the document identifier.
 * @property utilityType Name of the `UtilityType` this forecast targets.
 * @property dataPoints List of stored data points for the forecast.
 * @property computedAt Timestamp when the forecast was computed.
 */
@Serializable
data class ForecastedConsumptionDocument(
    @Suppress("ConstructorParameterNaming")
    val _id: String,
    val utilityType: String,
    val dataPoints: List<ForecastedDataPointDocument>,
    val computedAt: Instant,
)

/**
 * Persistence representation of a single forecasted data point.
 *
 * @property date Day the prediction refers to.
 * @property predictedValue Raw numeric predicted consumption value.
 */
@Serializable
data class ForecastedDataPointDocument(
    val date: LocalDate,
    val predictedValue: Double,
)
