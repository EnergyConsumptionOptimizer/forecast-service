package io.energyconsumptionoptimizer.forecastingservice.domain.value

import kotlinx.datetime.LocalDate

data class ForecastedDataPoint(
    val date: LocalDate,
    val predictedValue: ConsumptionValue,
)
