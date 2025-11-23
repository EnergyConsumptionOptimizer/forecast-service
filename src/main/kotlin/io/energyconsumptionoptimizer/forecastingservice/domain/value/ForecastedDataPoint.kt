package io.energyconsumptionoptimizer.forecastingservice.domain.value

import java.time.LocalDate

data class ForecastedDataPoint(
    val date: LocalDate,
    val predictedValue: ConsumptionValue,
)
