package io.energyconsumptionoptimizer.forecastservice.domain.value

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import io.energyconsumptionoptimizer.forecastservice.domain.DomainError
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

@ConsistentCopyVisibility
data class ForecastedDataSeries private constructor(
    val points: List<ForecastedDataPoint>,
) {
    val startDate: LocalDate get() = points.first().date
    val endDate: LocalDate get() = points.last().date
    val horizon: Int get() = startDate.daysUntil(endDate) + 1

    companion object {
        context(raise: Raise<DomainError.EmptyForecastDataSeries>)
        fun of(points: List<ForecastedDataPoint>): ForecastedDataSeries {
            raise.ensure(points.isNotEmpty()) { DomainError.EmptyForecastDataSeries }
            return ForecastedDataSeries(points.sorted())
        }
    }
}
