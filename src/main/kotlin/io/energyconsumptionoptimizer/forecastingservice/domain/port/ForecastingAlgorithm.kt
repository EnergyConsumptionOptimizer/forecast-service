package io.energyconsumptionoptimizer.forecastingservice.domain.port

import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastedDataPoint

interface ForecastingAlgorithm {
    val name: String

    suspend fun forecast(
        historicalData: List<HistoricalData>,
        horizon: Int,
    ): List<ForecastedDataPoint>

    fun validateInputs(
        historicalData: List<HistoricalData>,
        horizon: Int,
    ) {
        require(horizon > 0) { "Forecast horizon must be positive: $horizon" }
        require(historicalData.isNotEmpty()) { "Historical data cannot be empty" }

        val ratio = historicalData.size.toDouble() / (historicalData.size + horizon)
        require(ratio >= 0.7) {
            "Insufficient historical data for the requested horizon. " +
                "At least 70% of total data must be historical (got ${(ratio * 100).toInt()}%)."
        }
    }
}
