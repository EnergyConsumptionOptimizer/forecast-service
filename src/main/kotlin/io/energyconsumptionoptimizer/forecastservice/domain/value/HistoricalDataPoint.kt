package io.energyconsumptionoptimizer.forecastservice.domain.value

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import io.energyconsumptionoptimizer.forecastservice.domain.DomainError
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

@ConsistentCopyVisibility
data class HistoricalDataPoint private constructor(
    val date: LocalDate,
    val value: ConsumptionValue,
) : Comparable<HistoricalDataPoint> {
    override fun compareTo(other: HistoricalDataPoint): Int = this.date.compareTo(other.date)

    companion object {
        context(raise: Raise<DomainError.FutureHistoricalDate>)
        fun of(
            date: LocalDate,
            value: ConsumptionValue,
        ): HistoricalDataPoint {
            val today =
                kotlin.time.Clock.System
                    .todayIn(TimeZone.currentSystemDefault())
            raise.ensure(date <= today) { DomainError.FutureHistoricalDate(date) }
            return HistoricalDataPoint(date, value)
        }
    }
}
