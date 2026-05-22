package io.energyconsumptionoptimizer.forecastservice.infrastructure.metrics

import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.BusinessMetricsPort
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.metrics.LongCounter

class OtelBusinessMetrics : BusinessMetricsPort {
    private val meter = GlobalOpenTelemetry.getMeter("forecast-service")
    private val forecastComputationsTotal: LongCounter =
        meter
            .counterBuilder("forecast_computations_total")
            .setDescription("Total number of forecast computations")
            .build()

    override suspend fun recordForecastComputation() {
        forecastComputationsTotal.add(1)
    }
}
