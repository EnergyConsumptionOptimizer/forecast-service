package io.energyconsumptionoptimizer.forecastservice.domain

import kotlinx.datetime.LocalDate

sealed interface DomainError {
    data class InvalidConsumptionValue(
        val amount: Double,
    ) : DomainError

    data object EmptyHistoricalDataSeries : DomainError

    data class FutureHistoricalDate(
        val date: LocalDate,
    ) : DomainError

    data object EmptyForecastDataSeries : DomainError

    data class PastForecastDate(
        val date: LocalDate,
    ) : DomainError

    data class ForecastHorizonExceeded(
        val requestedDays: Int,
        val maxAllowed: Int,
    ) : DomainError

    data class UnknownUtilityType(
        val value: String,
    ) : DomainError
}
