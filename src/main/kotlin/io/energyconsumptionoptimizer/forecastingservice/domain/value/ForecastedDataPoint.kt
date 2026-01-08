package io.energyconsumptionoptimizer.forecastingservice.domain.value

import kotlinx.datetime.LocalDate

/**
 * A single daily forecasted data point.
 *
 * @property date the day this prediction refers to
 * @property predictedValue the predicted consumption value for the day
 */
data class ForecastedDataPoint(
    val date: LocalDate,
    val predictedValue: ConsumptionValue,
)
