package io.energyconsumptionoptimizer.forecastservice.component

import arrow.core.raise.either
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures
import io.energyconsumptionoptimizer.forecastservice.component.setup.ComponentTestSetup
import io.energyconsumptionoptimizer.forecastservice.component.setup.getAsAdmin
import io.energyconsumptionoptimizer.forecastservice.component.setup.testJson
import io.energyconsumptionoptimizer.forecastservice.component.setup.withTestServer
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.engine.TestAbortedException
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class ForecastComponentTest :
    BehaviorSpec({

        beforeSpec { ComponentTestSetup.startMongo() }
        afterSpec { ComponentTestSetup.stopMongo() }
        beforeEach { ComponentTestSetup.clearDatabase() }

        fun needsMongo() {
            if (!ComponentTestSetup.available) throw TestAbortedException("Embedded MongoDB not available")
        }

        fun extractForecastArray(json: String): JsonArray {
            val body = testJson.decodeFromString<JsonObject>(json)
            return body["forecasts"] as JsonArray
        }

        context("forecast retrieval") {

            Given("forecasts exist for multiple utilities") {
                When("the admin requests all forecasts") {
                    Then("every persisted forecast should be listed") {
                        needsMongo()

                        val repo = ComponentTestSetup.repository()
                        repo.save(DomainFixtures.aForecast(UtilityType.ELECTRICITY))
                        repo.save(DomainFixtures.aForecast(UtilityType.GAS))

                        testApplication {
                            withTestServer()
                            val response = client.getAsAdmin("/api/forecasts")

                            response.status shouldBe HttpStatusCode.OK
                            extractForecastArray(response.bodyAsText()) shouldHaveSize 2
                        }
                    }
                }
            }

            Given("no forecasts have been computed") {
                When("the admin requests all forecasts") {
                    Then("an empty collection should be returned") {
                        needsMongo()

                        testApplication {
                            withTestServer()
                            val response = client.getAsAdmin("/api/forecasts")

                            response.status shouldBe HttpStatusCode.OK
                            extractForecastArray(response.bodyAsText()) shouldHaveSize 0
                        }
                    }
                }
            }

            Given("the client is unauthenticated") {
                When("the client requests all forecasts") {
                    Then("access should be denied") {
                        needsMongo()

                        testApplication {
                            withTestServer()
                            val response = client.get("/api/forecasts")

                            response.status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            Given("a forecast exists for the requested utility") {
                When("the admin requests that utility's forecast") {
                    Then("its details should be returned") {
                        needsMongo()

                        ComponentTestSetup.repository().save(DomainFixtures.aForecast(UtilityType.GAS))

                        testApplication {
                            withTestServer()
                            val response = client.getAsAdmin("/api/forecasts/GAS")

                            response.status shouldBe HttpStatusCode.OK
                            val body = testJson.decodeFromString<JsonObject>(response.bodyAsText())
                            body["utilityType"]?.jsonPrimitive?.content shouldBe "GAS"
                        }
                    }
                }
            }

            Given("no forecast exists for the requested utility") {
                When("the admin requests that utility's forecast") {
                    Then("not found should be reported") {
                        needsMongo()

                        testApplication {
                            withTestServer()
                            val response = client.getAsAdmin("/api/forecasts/WATER")

                            response.status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }

            Given("the utility type is invalid") {
                When("the admin requests a forecast with an invalid type") {
                    Then("the request should be rejected") {
                        needsMongo()

                        testApplication {
                            withTestServer()
                            val response = client.getAsAdmin("/api/forecasts/INVALID")

                            response.status shouldBe HttpStatusCode.BadRequest
                        }
                    }
                }
            }
        }

        context("daily forecast computation") {

            Given("historical data is available") {
                When("the computation runs") {
                    Then("one forecast per utility type should be persisted") {
                        needsMongo()

                        val provider =
                            ComponentTestSetup.stubHistoricalDataProvider(
                                DomainFixtures.historicalData(30).points,
                            )
                        val service = ComponentTestSetup.createComputeService(provider)

                        val result = either { service.computeAll() }

                        result.shouldBeRight()
                        val stored = ComponentTestSetup.repository().findAll()
                        stored shouldHaveSize UtilityType.entries.size
                        stored.forEach { forecast ->
                            forecast.series.points shouldHaveSize forecast.series.horizon
                        }
                    }
                }
            }

            Given("the data provider returns no data") {
                When("the computation runs") {
                    Then("a computation failure should be reported") {
                        needsMongo()

                        val provider = ComponentTestSetup.stubHistoricalDataProvider(emptyList())
                        val service = ComponentTestSetup.createComputeService(provider)

                        val result = either { service.computeAll() }

                        result.shouldBeLeft()
                    }
                }
            }
        }
    })
