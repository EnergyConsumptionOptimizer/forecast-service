package io.energyconsumptionoptimizer.forecastservice.bootstrap

import io.energyconsumptionoptimizer.forecastservice.presentation.rest.middleware.AuthenticatedUser
import io.energyconsumptionoptimizer.forecastservice.presentation.rest.middleware.configureAuth
import io.energyconsumptionoptimizer.forecastservice.presentation.rest.middleware.configureErrorHandling
import io.energyconsumptionoptimizer.forecastservice.presentation.rest.routes.forecastRoutes
import io.energyconsumptionoptimizer.forecastservice.presentation.rest.routes.healthRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.principal
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.resources.Resources
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun Application.module(dependencies: Dependencies) {
    configureAuth()
    configurePlugins()
    configureErrorHandling()
    configureRouting(dependencies)
}

private fun Application.configurePlugins() {
    install(Resources)

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/api") }
        mdc("http.method") { it.request.httpMethod.value }
        mdc("http.path") { it.request.path() }
        mdc("user.id") {
            it.principal<AuthenticatedUser>()?.id ?: "anonymous"
        }
    }

    install(ContentNegotiation) {
        json(
            Json {
                isLenient = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            },
        )
    }
}

private fun Application.configureRouting(dependencies: Dependencies) {
    routing {
        healthRoutes()
        forecastRoutes(dependencies.forecastController)
    }
}
