package io.energyconsumptionoptimizer.forecastservice.infrastructure.metrics

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.metrics.LongCounter

object ErrorMetrics {
    private val meter = GlobalOpenTelemetry.getMeter("forecast-service")
    val errorsTotal: LongCounter =
        meter
            .counterBuilder("forecast_errors_total")
            .setDescription("Total number of errors in forecast service")
            .build()
}
