package io.energyconsumptionoptimizer.forecastservice

import io.energyconsumptionoptimizer.forecastservice.bootstrap.Config
import io.energyconsumptionoptimizer.forecastservice.bootstrap.Dependencies
import io.energyconsumptionoptimizer.forecastservice.bootstrap.module
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Server")

fun main() {
    val config = Config.fromEnv()
    val dependencies = Dependencies(config)

    startInfrastructure(dependencies)

    val server = embeddedServer(Netty, config.port) { module(dependencies) }
    onShutdown {
        dependencies.forecastScheduler.stop()
        dependencies.shutdown()
        server.stop(1000, 2000)
    }

    logger.info("Forecast Service started on port {}", config.port)
    server.start(wait = true)
}

private fun startInfrastructure(dependencies: Dependencies) {
    dependencies.otelSdk.start()
    dependencies.forecastScheduler.start()
}

private fun onShutdown(block: () -> Unit) {
    Runtime.getRuntime().addShutdownHook(
        Thread {
            logger.info("Shutting down...")
            block()
        },
    )
}
