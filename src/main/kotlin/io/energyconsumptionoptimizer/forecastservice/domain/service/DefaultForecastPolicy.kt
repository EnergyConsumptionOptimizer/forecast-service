package io.energyconsumptionoptimizer.forecastservice.domain.service

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import io.energyconsumptionoptimizer.forecastservice.domain.DomainError
import io.energyconsumptionoptimizer.forecastservice.domain.value.ForecastedDataSeries
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType

object DefaultForecastPolicy {
    fun maxHorizon(type: UtilityType): Int =
        when (type) {
            UtilityType.ELECTRICITY -> 100
            UtilityType.GAS -> 100
            UtilityType.WATER -> 100
        }

    context(raise: Raise<DomainError.ForecastHorizonExceeded>)
    fun validate(
        type: UtilityType,
        series: ForecastedDataSeries,
    ) {
        val maxAllowed = maxHorizon(type)

        raise.ensure(series.horizon <= maxAllowed) {
            DomainError.ForecastHorizonExceeded(series.horizon, maxAllowed)
        }
    }
}
