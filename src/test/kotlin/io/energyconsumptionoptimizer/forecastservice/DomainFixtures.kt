package io.energyconsumptionoptimizer.forecastservice

import arrow.core.raise.either
import io.energyconsumptionoptimizer.forecastservice.domain.entity.Forecast
import io.energyconsumptionoptimizer.forecastservice.domain.value.ConsumptionValue
import io.energyconsumptionoptimizer.forecastservice.domain.value.ForecastedDataPoint
import io.energyconsumptionoptimizer.forecastservice.domain.value.ForecastedDataSeries
import io.energyconsumptionoptimizer.forecastservice.domain.value.HistoricalDataPoint
import io.energyconsumptionoptimizer.forecastservice.domain.value.HistoricalDataSeries
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

object DomainFixtures {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val tomorrow = today.plus(1, DateTimeUnit.DAY)
    val yesterday = today.minus(1, DateTimeUnit.DAY)

    fun cv(amount: Double): ConsumptionValue =
        either { ConsumptionValue.of(amount) }.getOrNull() ?: error("Invalid fixture: consumption value must be >= 0, got $amount")

    fun forecastPoint(
        date: LocalDate,
        amount: Double,
    ): ForecastedDataPoint =
        either { ForecastedDataPoint.of(date, cv(amount)) }.getOrNull()
            ?: error("Invalid fixture: forecast date=$date, value=$amount")

    fun historicalPoint(
        date: LocalDate,
        amount: Double,
    ): HistoricalDataPoint =
        either { HistoricalDataPoint.of(date, cv(amount)) }.getOrNull()
            ?: error("Invalid fixture: historical date=$date, value=$amount")

    fun forecastSeries(vararg points: ForecastedDataPoint): ForecastedDataSeries =
        either { ForecastedDataSeries.of(points.toList()) }.getOrNull()
            ?: error("Invalid fixture: forecast series cannot be empty")

    fun historicalSeries(vararg points: HistoricalDataPoint): HistoricalDataSeries =
        either { HistoricalDataSeries.of(points.toList()) }.getOrNull()
            ?: error("Invalid fixture: historical series cannot be empty")

    fun aForecast(utilityType: UtilityType): Forecast =
        Forecast.create(
            utilityType = utilityType,
            newSeries =
                forecastSeries(
                    forecastPoint(today, 100.0),
                    forecastPoint(tomorrow, 150.0),
                ),
        )

    fun historicalDataPoints(count: Int): List<HistoricalDataPoint> =
        (1..count).map { daysAgo ->
            historicalPoint(today.minus(daysAgo, DateTimeUnit.DAY), 100.0)
        }

    fun historicalData(count: Int): HistoricalDataSeries = historicalSeries(*historicalDataPoints(count).toTypedArray())
}
