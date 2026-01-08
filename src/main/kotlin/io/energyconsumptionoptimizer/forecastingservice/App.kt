package io.energyconsumptionoptimizer.forecastingservice

import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.middleware.AuthMiddleware
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.middleware.configureAuthentication
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.middleware.configureErrorHandling
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.routes.forecastRoutes
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.routes.healthRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.resources.Resources
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun Application.module(dependencies: Dependencies) {
    configureSerialization()
    configureErrorHandling()
    configureAuthentication(dependencies.authMiddleware)
    configureRouting(dependencies)
}

private fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            },
        )
    }
    install(Resources)
}

private fun Application.configureRouting(dependencies: Dependencies) {
    routing {
        healthRoutes()
        forecastRoutes(dependencies.getForecastsUseCase)
    }
}
