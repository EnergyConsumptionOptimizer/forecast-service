package io.energyconsumptionoptimizer.forecastingservice.utils

import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.middleware.AuthMiddleware
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.middleware.configureAuthentication
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.middleware.configureErrorHandling
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.resources.Resources
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.io.IOException
import kotlinx.serialization.json.Json

fun ApplicationTestBuilder.configureTestApplication(
    authMiddleware: AuthMiddleware,
    routeConfig: Route.() -> Unit,
) {
    application {
        install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(io.ktor.server.resources.Resources)

        configureErrorHandling()
        configureAuthentication(authMiddleware)

        routing(routeConfig)
    }
}

fun ApplicationTestBuilder.createApiClient() =
    createClient {
        install(ContentNegotiation) { json() }
        install(Resources)
        install(HttpCookies)
    }

fun createMockAuthClient() =
    HttpClient(MockEngine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        engine {
            addHandler { request ->
                val isVerify = request.url.encodedPath.contains("/verify")
                val cookieHeader = request.headers["Cookie"]

                when {
                    cookieHeader?.contains("authToken=broken-service-token") == true -> {
                        throw IOException("Network error")
                    }

                    isVerify && cookieHeader?.contains("authToken=valid-token") == true -> {
                        respond(
                            content = """{"id": "user-123", "role": "USER"}""",
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }

                    else -> {
                        respond("", HttpStatusCode.Unauthorized)
                    }
                }
            }
        }
    }
