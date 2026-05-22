package io.energyconsumptionoptimizer.forecastservice.integration.rest

import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.presentation.WebApiErrorCode
import io.energyconsumptionoptimizer.forecastservice.presentation.rest.middleware.configureErrorHandling
import io.energyconsumptionoptimizer.forecastservice.presentation.rest.middleware.respondRaised
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.ParameterConversionException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.get
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class ErrorHandlerTest :
    DescribeSpec({

        val json = Json { ignoreUnknownKeys = true }

        fun ApplicationTestBuilder.setupApp() {
            application {
                install(ContentNegotiation) { json() }
                configureErrorHandling()
            }
        }

        describe("error handling middleware") {

            it("should respond 400 for parameter conversion failures") {
                testApplication {
                    setupApp()
                    routing {
                        get("/test") { throw ParameterConversionException("utilityType", "UtilityType") }
                    }

                    val response = client.get("/test")

                    response.status shouldBe HttpStatusCode.BadRequest
                    val body = json.decodeFromString<JsonObject>(response.bodyAsText())
                    body["code"]?.jsonPrimitive?.content shouldBe WebApiErrorCode.VALIDATION_ERROR
                }
            }

            it("should respond 500 for unexpected exceptions") {
                testApplication {
                    setupApp()
                    routing {
                        get("/test") { throw IllegalStateException("Something went wrong") }
                    }

                    val response = client.get("/test")

                    response.status shouldBe HttpStatusCode.InternalServerError
                    val body = json.decodeFromString<JsonObject>(response.bodyAsText())
                    body["code"]?.jsonPrimitive?.content shouldBe "INTERNAL_ERROR"
                }
            }

            it("should map DataProviderError to 502 Bad Gateway via respondRaised") {
                testApplication {
                    setupApp()
                    routing {
                        get("/test") {
                            call.respondRaised { raise(ApplicationError.DataProviderError("Monitoring service down")) }
                        }
                    }

                    val response = client.get("/test")

                    response.status shouldBe HttpStatusCode.BadGateway
                    val body = json.decodeFromString<JsonObject>(response.bodyAsText())
                    body["code"]?.jsonPrimitive?.content shouldBe "EXTERNAL_SERVICE_ERROR"
                }
            }
        }
    })
