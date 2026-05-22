package io.energyconsumptionoptimizer.forecastservice.integration.http

import arrow.core.raise.either
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import io.energyconsumptionoptimizer.forecastservice.infrastructure.http.MonitoringResponse
import io.energyconsumptionoptimizer.forecastservice.infrastructure.http.MonitoringServiceClient
import io.energyconsumptionoptimizer.forecastservice.infrastructure.http.UtilityConsumptionPointDto
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.beGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MonitoringServiceClientIntegrationTest :
    DescribeSpec({

        val json =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

        val validResponseBody =
            json.encodeToString(
                MonitoringResponse(
                    (1..3).map { i ->
                        UtilityConsumptionPointDto(
                            value = 100.0 + i * 10,
                            timestamp = kotlin.time.Instant.parse("2026-01-0${i}T00:00:00Z"),
                        )
                    },
                ),
            )

        describe("MonitoringServiceClient") {

            describe("fetchByUtility") {

                it("should parse a valid JSON response and return data points sorted by date") {
                    testApplication {
                        routing {
                            get("/api/internal/measurements/{utilityType}") {
                                call.respondText(validResponseBody, ContentType.Application.Json)
                            }
                        }

                        val httpClient =
                            createClient {
                                install(ContentNegotiation) { json(json) }
                            }
                        val client = MonitoringServiceClient(httpClient, "")

                        val result = either { client.fetchByUtility(UtilityType.ELECTRICITY) }.shouldBeRight()

                        result.predictions shouldHaveSize 3
                        result.predictions.zipWithNext().forEach { (a, b) -> (a.date <= b.date).shouldBeTrue() }
                        result.predictions.forEach { it.value shouldBe beGreaterThan(0.0) }
                    }
                }

                it("should raise DataProviderError when the server returns an error status") {
                    testApplication {
                        routing {
                            get("/api/internal/measurements/{utilityType}") {
                                call.respondText("Service unavailable", status = HttpStatusCode.ServiceUnavailable)
                            }
                        }

                        val httpClient =
                            createClient {
                                install(ContentNegotiation) { json(json) }
                            }
                        val client = MonitoringServiceClient(httpClient, "")

                        val error = either { client.fetchByUtility(UtilityType.ELECTRICITY) }.shouldBeLeft()
                        error.shouldBeInstanceOf<ApplicationError.DataProviderError>()
                    }
                }

                it("should raise DataProviderError when the response body is malformed") {
                    testApplication {
                        routing {
                            get("/api/internal/measurements/{utilityType}") {
                                call.respondText("{malformed", ContentType.Application.Json)
                            }
                        }

                        val httpClient =
                            createClient {
                                install(ContentNegotiation) { json(json) }
                            }
                        val client = MonitoringServiceClient(httpClient, "")

                        val error = either { client.fetchByUtility(UtilityType.ELECTRICITY) }.shouldBeLeft()
                        error.shouldBeInstanceOf<ApplicationError.DataProviderError>()
                    }
                }
            }
        }
    })
