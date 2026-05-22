package io.energyconsumptionoptimizer.forecastservice.unit.infrastructure.http

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
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Instant

class MonitoringServiceClientTest :
    DescribeSpec({

        val anyUtility = UtilityType.ELECTRICITY
        val baseUrl = "http://monitoring-service"
        val json =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

        fun newClient(mockEngine: MockEngine) =
            MonitoringServiceClient(
                HttpClient(mockEngine) { install(ContentNegotiation) { json(json) } },
                baseUrl,
            )

        describe("MonitoringServiceClient") {

            describe("fetchByUtility") {

                context("given a successful HTTP response with valid data") {
                    it("should call the correct URL, parse the JSON body, and return mapped DataPoints sorted by date") {
                        val responseBody =
                            json.encodeToString(
                                MonitoringResponse(
                                    utilityConsumptions =
                                        (0 until 3).map { i ->
                                            UtilityConsumptionPointDto(
                                                value = 100.0 + i * 10,
                                                timestamp = Instant.parse("2026-01-${(i + 1).toString().padStart(2, '0')}T00:00:00Z"),
                                            )
                                        },
                                ),
                            )
                        val mockEngine =
                            MockEngine { request ->
                                request.url.encodedPath shouldContain "/api/internal/measurements/${anyUtility.name}"
                                respond(
                                    content = responseBody,
                                    status = HttpStatusCode.OK,
                                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                                )
                            }
                        val client = newClient(mockEngine)

                        val result = either { client.fetchByUtility(anyUtility) }.shouldBeRight()

                        result.predictions shouldHaveSize 3
                        val dates = result.predictions.map { it.date }
                        dates.zipWithNext().forEach { (a, b) -> (a <= b).shouldBeTrue() }
                        result.predictions.forEach { point ->
                            point.value shouldBe beGreaterThan(0.0)
                        }
                    }
                }

                context("given a non-success HTTP response status") {
                    it("should raise DataProviderError") {
                        val mockEngine =
                            MockEngine {
                                respond(content = "", status = HttpStatusCode.ServiceUnavailable)
                            }
                        val client = newClient(mockEngine)

                        val error = either { client.fetchByUtility(anyUtility) }.shouldBeLeft()

                        error.shouldBeInstanceOf<ApplicationError.DataProviderError>()
                    }
                }

                context("given an unreachable HTTP endpoint") {
                    it("should raise DataProviderError") {
                        val mockEngine = MockEngine { throw java.io.IOException("Connection refused") }
                        val client = newClient(mockEngine)

                        val error = either { client.fetchByUtility(anyUtility) }.shouldBeLeft()

                        error.shouldBeInstanceOf<ApplicationError.DataProviderError>()
                    }
                }

                context("given a response with malformed JSON body") {
                    it("should raise DataProviderError") {
                        val mockEngine =
                            MockEngine {
                                respond(
                                    content = "{not valid json}",
                                    status = HttpStatusCode.OK,
                                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                                )
                            }
                        val client = newClient(mockEngine)

                        val error = either { client.fetchByUtility(anyUtility) }.shouldBeLeft()

                        error.shouldBeInstanceOf<ApplicationError.DataProviderError>()
                    }
                }
            }
        }
    })
