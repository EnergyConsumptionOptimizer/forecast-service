package io.energyconsumptionoptimizer.forecastservice.application.ports.dto

data class AlgorithmPrediction(
    val predictions: List<DataPoint>,
)
