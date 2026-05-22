package io.energyconsumptionoptimizer.forecastservice.domain.value

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import io.energyconsumptionoptimizer.forecastservice.domain.DomainError

@ConsistentCopyVisibility
data class HistoricalDataSeries private constructor(
    val points: List<HistoricalDataPoint>,
) {
    val size: Int get() = points.size

    companion object {
        context(raise: Raise<DomainError.EmptyHistoricalDataSeries>)
        fun of(points: List<HistoricalDataPoint>): HistoricalDataSeries {
            raise.ensure(points.isNotEmpty()) { DomainError.EmptyHistoricalDataSeries }
            return HistoricalDataSeries(points.sorted())
        }
    }
}
