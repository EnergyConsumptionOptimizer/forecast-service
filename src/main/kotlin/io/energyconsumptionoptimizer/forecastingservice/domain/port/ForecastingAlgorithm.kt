package io.energyconsumptionoptimizer.forecastingservice.domain.port

import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastedDataPoint

interface ForecastingAlgorithm {
    val name: String

    companion object {
        const val MIN_RATIO = 0.7
        const val PERCENT = 100
    }

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
        require(ratio >= MIN_RATIO) {
            "Insufficient historical data for the requested horizon. " +
                "At least ${(MIN_RATIO * PERCENT).toInt()}% historical (got ${(ratio * PERCENT).toInt()}%)."
        }
    }
}
