package io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi

import io.energyconsumptionoptimizer.forecastingservice.application.usecases.GetForecastsUseCase
import io.energyconsumptionoptimizer.forecastingservice.domain.ForecastedConsumption
import io.energyconsumptionoptimizer.forecastingservice.domain.port.ForecastRepository
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastId
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.middleware.authenticated
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.middleware.configureAuthentication
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.middleware.configureErrorHandling
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.resources.ForecastResource
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.resources.HealthResource
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.routes.HealthResponse
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.routes.forecastRoutes
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.routes.healthRoutes
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.plugins.resources.get
import io.ktor.client.request.cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json

class ForecastApiTest :
    StringSpec({

        val mockAuthClient = createMockAuthClient()
        val useCase = GetForecastsUseCase(FakeForecastRepository())

        "GET /api/health should return 200 OK" {
            testApplication {
                setupApp(mockAuthClient) { healthRoutes() }

                val response = createApiClient().get(HealthResource())

                response.status shouldBe HttpStatusCode.OK
                response.body<HealthResponse>().status shouldBe "UP"
            }
        }

        "GET /api/forecasts should return 401 Unauthorized without cookies" {
            testApplication {
                setupApp(mockAuthClient) {
                    authenticated { forecastRoutes(useCase) }
                }

                val response = createApiClient().get(ForecastResource())

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "GET /api/forecasts should return 200 OK with valid cookie" {
            testApplication {
                setupApp(mockAuthClient) {
                    authenticated { forecastRoutes(useCase) }
                }

                val client = createApiClient()
                val response =
                    client.get(ForecastResource()) {
                        cookie("authToken", "valid-token")
                    }

                response.status shouldBe HttpStatusCode.OK
            }
        }
    })

private fun ApplicationTestBuilder.setupApp(
    authClient: HttpClient,
    routing: Route.() -> Unit,
) {
    environment {
        config = MapApplicationConfig("user.service.uri" to "http://mock-user-service")
    }
    application {
        install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) { json() }
        install(io.ktor.server.resources.Resources)
        configureErrorHandling()
        configureAuthentication(authClient)
        routing(routing)
    }
}

private fun ApplicationTestBuilder.createApiClient() =
    createClient {
        install(ContentNegotiation) { json() }
        install(Resources)
        install(HttpCookies)
    }

private fun createMockAuthClient() =
    HttpClient(MockEngine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        engine {
            addHandler { request ->
                val isVerify = request.url.encodedPath.contains("/verify")
                val hasToken = request.headers[HttpHeaders.Cookie]?.contains("valid-token") == true

                if (isVerify && hasToken) {
                    respond(
                        content = """{"id": "user-123", "role": "USER"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                } else {
                    respond("", HttpStatusCode.Unauthorized)
                }
            }
        }
    }

private class FakeForecastRepository : ForecastRepository {
    override suspend fun findAll(): List<ForecastedConsumption> = emptyList()

    override suspend fun findByUtility(utilityType: UtilityType): ForecastedConsumption? = null

    override suspend fun save(forecast: ForecastedConsumption): ForecastedConsumption = TODO()

    override suspend fun findById(id: ForecastId): ForecastedConsumption = TODO()

    override suspend fun remove(forecast: ForecastedConsumption): Boolean = TODO()

    override suspend fun removeById(id: ForecastId): Boolean = TODO()

    override suspend fun removeByUtility(utilityType: UtilityType): Boolean = TODO()
}
