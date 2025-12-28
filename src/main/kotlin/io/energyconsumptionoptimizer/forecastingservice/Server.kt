package io.energyconsumptionoptimizer.forecastingservice

import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlin.system.exitProcess

private const val DEFAULT_PORT = 8080
private const val SHUTDOWN_GRACE_PERIOD_MS = 1000L
private const val SHUTDOWN_TIMEOUT_MS = 2000L

fun main() {
    try {
        val config = loadConfiguration()
        val dependencies = Dependencies(config)

        initializeDatabase(dependencies)

        startScheduler(dependencies)

        val server =

            createServer(dependencies)

        registerShutdownHook(dependencies, server)
        startServer(server)
    } catch (_: Exception) {
        exitProcess(1)
    }
}

private fun loadConfiguration() =
    AppConfig(
        mongoUri = System.getenv("MONGO_URI") ?: "mongodb://localhost:27017",
        mongoDatabase = System.getenv("MONGO_DATABASE") ?: "forecasting",
        monitoringServiceUrl = System.getenv("MONITORING_SERVICE_URL") ?: "http://monitoring-service:3000",
        thresholdServiceUrl = System.getenv("THRESHOLD_SERVICE_URL") ?: "http://threshold-service:3000",
        lookbackDays = System.getenv("LOOKBACK_DAYS")?.toIntOrNull() ?: 30,
        schedulerHour = System.getenv("FORECAST_HOUR")?.toIntOrNull() ?: 0,
        schedulerMinute = System.getenv("FORECAST_MINUTE")?.toIntOrNull() ?: 0,
    )

private fun initializeDatabase(dependencies: Dependencies) {
    dependencies.mongoClient
}

private fun startScheduler(dependencies: Dependencies) {
    runCatching { dependencies.forecastScheduler.start() }
}

private fun createServer(dependencies: Dependencies) =
    embeddedServer(Netty, port = System.getenv("SERVER_PORT")?.toIntOrNull() ?: DEFAULT_PORT, host = "0.0.0.0") {
        module(dependencies)
    }

private fun registerShutdownHook(
    dependencies: Dependencies,
    server: EmbeddedServer<NettyApplicationEngine, *>?,
) {
    Runtime.getRuntime().addShutdownHook(
        Thread {
            dependencies.shutdown()
            server?.stop(SHUTDOWN_GRACE_PERIOD_MS, SHUTDOWN_TIMEOUT_MS)
        },
    )
}

private fun startServer(server: EmbeddedServer<NettyApplicationEngine, *>?) {
    if (server != null) {
        server.start(wait = true)
    } else {
        Thread.currentThread().join()
    }
}
