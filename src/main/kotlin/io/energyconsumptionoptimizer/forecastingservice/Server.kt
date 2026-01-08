package io.energyconsumptionoptimizer.forecastingservice

import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlin.system.exitProcess

private const val SHUTDOWN_GRACE_PERIOD_MS = 1000L
private const val SHUTDOWN_TIMEOUT_MS = 2000L
private val PORT = System.getenv("PORT")?.toIntOrNull() ?: 3000

/**
 * Application entrypoint executed by the JVM.
 *
 * Loads runtime configuration, initializes dependencies, database, and scheduler,
 * then starts the HTTP server. Exits the process with a non-zero code on error.
 */
fun main() {
    try {
        val config = loadConfiguration()
        val dependencies = Dependencies(config)

        initializeDatabase(dependencies)
        startScheduler(dependencies)

        val server = createServer(dependencies)
        registerShutdownHook(dependencies, server)
        startServer(server)
    } catch (_: Exception) {
        exitProcess(1)
    }
}

private fun loadConfiguration(): AppConfig {
    val mongoHost = getEnv("MONGODB_HOST", "localhost")
    val mongoPort = getEnvInt("MONGODB_PORT", 27017)
    val mongoDbName = getEnv("MONGO_DB", "forecasting")

    val userHost = getEnv("USER_SERVICE_HOST", "user-service")
    val userPort = getEnvInt("USER_SERVICE_PORT", 3000)

    val monitoringHost = getEnv("MONITORING_SERVICE_HOST", "monitoring-service")
    val monitoringPort = getEnvInt("MONITORING_SERVICE_PORT", 3000)

    val thresholdHost = getEnv("THRESHOLD_SERVICE_HOST", "threshold-service")
    val thresholdPort = getEnvInt("THRESHOLD_SERVICE_PORT", 3000)

    return AppConfig(
        mongoUri = "mongodb://$mongoHost:$mongoPort",
        mongoDatabase = mongoDbName,
        userServiceUrl = getEnv("USER_SERVICE_URL", "http://$userHost:$userPort"),
        monitoringServiceUrl = getEnv("MONITORING_SERVICE_URL", "http://$monitoringHost:$monitoringPort"),
        thresholdServiceUrl = getEnv("THRESHOLD_SERVICE_URL", "http://$thresholdHost:$thresholdPort"),
        lookbackDays = getEnvInt("LOOKBACK_DAYS", 30),
        schedulerHour = getEnvInt("FORECAST_HOUR", 0),
        schedulerMinute = getEnvInt("FORECAST_MINUTE", 0),
    )
}

private fun getEnv(
    key: String,
    default: String,
): String = System.getenv(key) ?: default

private fun getEnvInt(
    key: String,
    default: Int,
): Int = System.getenv(key)?.toIntOrNull() ?: default

private fun initializeDatabase(dependencies: Dependencies) {
    dependencies.mongoClient
}

private fun startScheduler(dependencies: Dependencies) {
    runCatching { dependencies.forecastScheduler.start() }
}

private fun createServer(dependencies: Dependencies) =
    embeddedServer(Netty, PORT) {
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
