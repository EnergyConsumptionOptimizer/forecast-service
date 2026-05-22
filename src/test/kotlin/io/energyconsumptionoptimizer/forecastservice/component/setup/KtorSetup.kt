package io.energyconsumptionoptimizer.forecastservice.component.setup

import io.energyconsumptionoptimizer.forecastservice.presentation.rest.middleware.configureAuth
import io.energyconsumptionoptimizer.forecastservice.presentation.rest.middleware.configureErrorHandling
import io.energyconsumptionoptimizer.forecastservice.presentation.rest.routes.forecastRoutes
import io.energyconsumptionoptimizer.forecastservice.presentation.rest.routes.healthRoutes
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.resources.Resources
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.serialization.json.Json

val testJson =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

fun ApplicationTestBuilder.withTestServer() {
    application {
        install(ContentNegotiation) { json(testJson) }
        install(Resources)
        configureAuth()
        configureErrorHandling()
    }
    routing {
        healthRoutes()
        forecastRoutes(ComponentTestSetup.forecastController)
    }
}

suspend fun HttpClient.getAsAdmin(path: String) =
    get(path) {
        header("x-user-id", "admin-123")
        header("x-user-role", "ADMIN")
        header("x-user-username", "admin")
    }
