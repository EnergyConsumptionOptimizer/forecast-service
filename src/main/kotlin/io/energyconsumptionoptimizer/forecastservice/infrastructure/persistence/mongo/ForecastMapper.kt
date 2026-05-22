package io.energyconsumptionoptimizer.forecastservice.infrastructure.persistence.mongo

import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.withError
import io.energyconsumptionoptimizer.forecastservice.domain.DomainError
import io.energyconsumptionoptimizer.forecastservice.domain.entity.Forecast
import io.energyconsumptionoptimizer.forecastservice.domain.value.ConsumptionValue
import io.energyconsumptionoptimizer.forecastservice.domain.value.ForecastedDataPoint
import io.energyconsumptionoptimizer.forecastservice.domain.value.ForecastedDataSeries
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import io.energyconsumptionoptimizer.forecastservice.logger

object ForecastMapper {
    private val logger = logger()

    fun toDocument(forecast: Forecast): ForecastDocument =
        ForecastDocument(
            utilityType = forecast.utilityType.name,
            dataPoints =
                forecast.series.points.map { point ->
                    DataPointDocument(
                        date = point.date,
                        value = point.value.amount,
                    )
                },
            computedAt = forecast.computedAt.toEpochMilliseconds(),
        )

    fun toDomain(doc: ForecastDocument): Forecast? =
        either {
            val utilityType = parseUtilityType(doc.utilityType)
            val dataPoints =
                doc.dataPoints.map { point ->
                    ForecastedDataPoint.of(
                        date = point.date,
                        value =
                            withError(
                                { e: DomainError.InvalidConsumptionValue -> e as DomainError },
                                { with(ConsumptionValue) { of(point.value) } },
                            ),
                    )
                }
            val forecastSeries =
                withError(
                    { e: DomainError -> e },
                    { ForecastedDataSeries.of(dataPoints) },
                )
            Forecast.restore(
                utilityType = utilityType,
                dataPoints = forecastSeries,
                at = kotlin.time.Instant.fromEpochMilliseconds(doc.computedAt),
            )
        }.fold(
            ifLeft = { error ->
                logger.warn("Skipping invalid forecast document utilityType={} error={}", doc.utilityType, error)
                null
            },
            ifRight = { it },
        )

    private fun Raise<DomainError>.parseUtilityType(value: String): UtilityType =
        UtilityType.entries.firstOrNull { it.name == value.trim().uppercase() }
            ?: raise(DomainError.UnknownUtilityType(value))
}
