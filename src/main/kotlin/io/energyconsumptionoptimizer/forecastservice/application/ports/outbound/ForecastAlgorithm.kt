package io.energyconsumptionoptimizer.forecastservice.application.ports.outbound

import arrow.core.raise.Raise
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.application.ports.dto.AlgorithmPrediction
import io.energyconsumptionoptimizer.forecastservice.domain.value.HistoricalDataSeries

interface ForecastAlgorithm {
    context(raise: Raise<ApplicationError.AlgorithmError>)
    suspend fun forecast(
        historicalData: HistoricalDataSeries,
        horizon: Int,
    ): AlgorithmPrediction
}
