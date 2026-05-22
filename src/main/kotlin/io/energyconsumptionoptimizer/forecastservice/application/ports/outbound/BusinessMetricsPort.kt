package io.energyconsumptionoptimizer.forecastservice.application.ports.outbound

interface BusinessMetricsPort {
    suspend fun recordForecastComputation()
}
