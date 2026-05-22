package io.energyconsumptionoptimizer.forecastservice

import io.energyconsumptionoptimizer.forecastservice.logger
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk

class OtelSdk {
    private var sdk: OpenTelemetrySdk? = null
    private val logger = logger()

    fun start() {
        try {
            val autoConfigured = AutoConfiguredOpenTelemetrySdk.initialize()
            sdk = autoConfigured.openTelemetrySdk
            logger.info("OpenTelemetry SDK started")
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            logger.error("Failed to start OpenTelemetry SDK", e)
            try {
                GlobalOpenTelemetry.resetForTest()
            } catch (_: Exception) {
                // best effort reset
            }
        }
    }

    fun shutdown() {
        try {
            sdk?.close()
            logger.info("OpenTelemetry SDK shut down")
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            logger.error("Error shutting down OpenTelemetry SDK", e)
        }
    }
}
