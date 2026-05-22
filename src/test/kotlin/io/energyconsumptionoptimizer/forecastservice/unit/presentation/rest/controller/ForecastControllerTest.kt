package io.energyconsumptionoptimizer.forecastservice.unit.presentation.rest.controller

import arrow.core.raise.Raise
import arrow.core.raise.either
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.application.ports.dto.DataPoint
import io.energyconsumptionoptimizer.forecastservice.application.ports.inbound.GetForecastsService
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import io.energyconsumptionoptimizer.forecastservice.presentation.rest.controller.ForecastController
import io.energyconsumptionoptimizer.forecastservice.presentation.rest.middleware.configureErrorHandling
import io.energyconsumptionoptimizer.forecastservice.presentation.rest.middleware.respondRaised
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.get
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock
import io.energyconsumptionoptimizer.forecastservice.application.ports.dto.ForecastResponse as AppForecastResponse

class ForecastControllerTest :
    DescribeSpec({

        val serviceMock = mockk<GetForecastsService>()
        val controller = ForecastController(serviceMock)
        val testJson = Json { ignoreUnknownKeys = true }

        fun buildAppForecastResponse(utilityType: UtilityType) =
            AppForecastResponse(
                utilityType = utilityType,
                dataPoints = listOf(DataPoint(LocalDate(2024, 1, 1), 100.0)),
                computedAt = Clock.System.now(),
            )

        fun ApplicationTestBuilder.withController() {
            application {
                install(ContentNegotiation) { json(testJson) }
                configureErrorHandling()
            }
            routing {
                get("/api/forecasts") {
                    call.respondRaised { controller.getAll() }
                }
                get("/api/forecasts/{utilityType}") {
                    val type = call.parameters["utilityType"] ?: ""
                    val utilityType =
                        either { UtilityType.of(type) }
                            .getOrNull() ?: throw BadRequestException("Invalid utility type: $type")
                    call.respondRaised { controller.getByUtility(utilityType) }
                }
            }
        }

        describe("GET /api/forecasts") {

            it("returns 200 OK with all forecasts") {
                val forecasts =
                    listOf(
                        buildAppForecastResponse(UtilityType.ELECTRICITY),
                        buildAppForecastResponse(UtilityType.GAS),
                    )
                coEvery {
                    with(any<Raise<ApplicationError>>()) { serviceMock.getAll() }
                } returns forecasts

                testApplication {
                    withController()
                    val response = client.get("/api/forecasts")

                    response.status shouldBe HttpStatusCode.OK
                    val body = testJson.decodeFromString<JsonObject>(response.bodyAsText())
                    val items = body["forecasts"] as JsonArray
                    items shouldHaveSize 2
                    (items[0] as JsonObject)["id"]?.jsonPrimitive?.content shouldBe "ELECTRICITY"
                }
            }

            it("returns 200 OK with empty list when no forecasts exist") {
                coEvery {
                    with(any<Raise<ApplicationError>>()) { serviceMock.getAll() }
                } returns emptyList()

                testApplication {
                    withController()
                    val response = client.get("/api/forecasts")

                    response.status shouldBe HttpStatusCode.OK
                    val body = testJson.decodeFromString<JsonObject>(response.bodyAsText())
                    (body["forecasts"] as JsonArray) shouldHaveSize 0
                }
            }

            it("returns 502 Bad Gateway when service raises an external error") {
                coEvery {
                    with(any<Raise<ApplicationError>>()) { serviceMock.getAll() }
                } answers {
                    val raiseContext = firstArg<Raise<ApplicationError>>()
                    raiseContext.raise(ApplicationError.AlgorithmError("boom"))
                }

                testApplication {
                    withController()
                    val response = client.get("/api/forecasts")

                    response.status shouldBe HttpStatusCode.BadGateway
                }
            }
        }

        describe("GET /api/forecasts/{utilityType}") {

            it("returns 200 OK with the requested forecast") {
                val forecast = buildAppForecastResponse(UtilityType.WATER)
                coEvery {
                    with(any<Raise<ApplicationError>>()) { serviceMock.getByUtility(UtilityType.WATER) }
                } returns forecast

                testApplication {
                    withController()
                    val response = client.get("/api/forecasts/WATER")

                    response.status shouldBe HttpStatusCode.OK
                    val body = testJson.decodeFromString<JsonObject>(response.bodyAsText())
                    body["id"]?.jsonPrimitive?.content shouldBe "WATER"
                    (body["dataPoints"] as JsonArray) shouldHaveSize 1
                }
            }

            it("returns 404 NotFound when no forecast exists") {
                coEvery {
                    with(any<Raise<ApplicationError>>()) { serviceMock.getByUtility(UtilityType.GAS) }
                } answers {
                    val raiseContext = firstArg<Raise<ApplicationError>>()
                    raiseContext.raise(ApplicationError.ForecastNotFound(UtilityType.GAS))
                }

                testApplication {
                    withController()
                    val response = client.get("/api/forecasts/GAS")

                    response.status shouldBe HttpStatusCode.NotFound
                }
            }

            it("returns 400 BadRequest for an invalid utility type") {
                testApplication {
                    withController()
                    val response = client.get("/api/forecasts/INVALID")

                    response.status shouldBe HttpStatusCode.BadRequest
                }
            }
        }
    })
