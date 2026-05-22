package io.energyconsumptionoptimizer.forecastservice.application.ports.dto

data class HistoricalDataProvided(
    val predictions: List<DataPoint>,
)
