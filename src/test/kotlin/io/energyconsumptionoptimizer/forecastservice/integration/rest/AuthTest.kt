package io.energyconsumptionoptimizer.forecastservice.integration.rest

import io.energyconsumptionoptimizer.forecastservice.presentation.WebApiErrorCode
import io.energyconsumptionoptimizer.forecastservice.presentation.rest.middleware.AuthenticatedUser
import io.energyconsumptionoptimizer.forecastservice.presentation.rest.middleware.configureAuth
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class AuthTest :
    DescribeSpec({

        val json = Json { ignoreUnknownKeys = true }

        fun ApplicationTestBuilder.setupApp() {
            application {
                install(ContentNegotiation) { json() }
                configureAuth()
            }
            routing {
                authenticate("header-auth") {
                    get("/protected") {
                        val user = call.principal<AuthenticatedUser>()
                        call.respondText("${user?.id}:${user?.username}:${user?.role}")
                    }
                }
            }
        }

        describe("header-based authentication") {

            it("should extract user identity from x-user-* headers") {
                testApplication {
                    setupApp()

                    val response =
                        client.get("/protected") {
                            header("x-user-id", "user-123")
                            header("x-user-username", "johndoe")
                            header("x-user-role", "ADMIN")
                        }

                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldBe "user-123:johndoe:ADMIN"
                }
            }

            it("should default the role to HOUSEHOLD when x-user-role is missing") {
                testApplication {
                    setupApp()

                    val response =
                        client.get("/protected") {
                            header("x-user-id", "user-456")
                        }

                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldBe "user-456::HOUSEHOLD"
                }
            }

            it("should reject requests when x-user-id is missing") {
                testApplication {
                    setupApp()

                    val response = client.get("/protected")

                    response.status shouldBe HttpStatusCode.Unauthorized
                    val body = json.decodeFromString<JsonObject>(response.bodyAsText())
                    body["code"]?.jsonPrimitive?.content shouldBe WebApiErrorCode.AUTH_REQUIRED
                }
            }
        }
    })
