package io.energyconsumptionoptimizer.forecastingservice.domain.port

import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastedDataPoint

/**
 * Strategy contract for forecasting algorithms used by the service.
 *
 * Implementations produce a sequence of `ForecastedDataPoint` values
 * given historical observations and a forecast horizon.
 */
interface ForecastingAlgorithm {
    /**
     * Human-readable algorithm name.
     */
    val name: String

    companion object {
        const val MIN_RATIO = 0.7
        const val PERCENT = 100
    }

    /**
     * Produce forecasted data points for the given `historicalData` and `horizon` (days).
     *
     * @param historicalData historical observations used as input
     * @param horizon number of days into the future to forecast (must be > 0)
     * @return ordered list of [ForecastedDataPoint]
     * @throws IllegalArgumentException When inputs are invalid (see [validateInputs]).
     */
    suspend fun forecast(
        historicalData: List<HistoricalData>,
        horizon: Int,
    ): List<ForecastedDataPoint>

    /**
     * Validate inputs commonly required by forecasting algorithms.
     * Throws `IllegalArgumentException` when inputs are invalid.
     */
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
