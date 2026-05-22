package io.energyconsumptionoptimizer.forecastservice.application.ports.dto

import kotlinx.datetime.LocalDate

data class DataPoint(
    val date: LocalDate,
    val value: Double,
)
