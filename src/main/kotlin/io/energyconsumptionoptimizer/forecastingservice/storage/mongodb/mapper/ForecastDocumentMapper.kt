package io.energyconsumptionoptimizer.forecastingservice.storage.mongodb.mapper

import io.energyconsumptionoptimizer.forecastingservice.domain.ForecastedConsumption
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ConsumptionValue
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastId
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastedDataPoint
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType
import io.energyconsumptionoptimizer.forecastingservice.storage.mongodb.document.ForecastedConsumptionDocument
import io.energyconsumptionoptimizer.forecastingservice.storage.mongodb.document.ForecastedDataPointDocument

/**
 * Convert a domain [ForecastedConsumption] into a MongoDB persistence document.
 *
 * @receiver Source domain forecast to convert.
 * @return [ForecastedConsumptionDocument] ready for serialization and storage.
 */
fun ForecastedConsumption.toDocument(): ForecastedConsumptionDocument =
    ForecastedConsumptionDocument(
        _id = id.toString(),
        utilityType = utilityType.name,
        dataPoints =
            forecastedDataPoints.map {
                ForecastedDataPointDocument(
                    date = it.date,
                    predictedValue = it.predictedValue.amount,
                )
            },
        computedAt = computedAt,
    )

/**
 * Reconstruct a domain [ForecastedConsumption] from a persistence document.
 *
 * @receiver Document representation loaded from storage.
 * @return Restored [ForecastedConsumption] domain object.
 */
fun ForecastedConsumptionDocument.toDomain(): ForecastedConsumption =
    ForecastedConsumption.fromPersistence(
        id = ForecastId.from(_id),
        utilityType = UtilityType.fromString(utilityType),
        forecastedDataPoints =
            dataPoints.map {
                ForecastedDataPoint(
                    date = it.date,
                    predictedValue = ConsumptionValue.of(it.predictedValue),
                )
            },
        computedAt = computedAt,
    )
