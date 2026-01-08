package io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi

import io.energyconsumptionoptimizer.forecastingservice.application.usecases.GetForecastsUseCase
import io.energyconsumptionoptimizer.forecastingservice.domain.ForecastedConsumption
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ConsumptionValue
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastedDataPoint
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.middleware.AuthMiddleware
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.resources.ForecastResource
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.resources.HealthResource
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.routes.HealthResponse
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.routes.forecastRoutes
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.routes.healthRoutes
import io.energyconsumptionoptimizer.forecastingservice.presentation.dto.ForecastListResponse
import io.energyconsumptionoptimizer.forecastingservice.presentation.dto.UtilityTypeDto
import io.energyconsumptionoptimizer.forecastingservice.utils.configureTestApplication
import io.energyconsumptionoptimizer.forecastingservice.utils.createApiClient
import io.energyconsumptionoptimizer.forecastingservice.utils.fakes.FakeForecastRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.client.request.cookie
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import kotlinx.datetime.LocalDate

class ForecastApiTest :
    StringSpec({

        val mockAuthMiddleware = mockk<AuthMiddleware>(relaxed = true)
        val fakeRepository = FakeForecastRepository()
        val useCase = GetForecastsUseCase(fakeRepository)

        beforeSpec {
            fakeRepository.save(
                ForecastedConsumption.create(
                    utilityType = UtilityType.ELECTRICITY,
                    forecastedDataPoints =
                        listOf(
                            ForecastedDataPoint(
                                date = LocalDate(2024, 1, 1),
                                predictedValue = ConsumptionValue.of(100.0),
                            ),
                        ),
                ),
            )
            fakeRepository.save(
                ForecastedConsumption.create(
                    utilityType = UtilityType.GAS,
                    forecastedDataPoints =
                        listOf(
                            ForecastedDataPoint(
                                date = LocalDate(2024, 1, 1),
                                predictedValue = ConsumptionValue.of(50.0),
                            ),
                        ),
                ),
            )
        }

        "GET /health should return 200 OK" {
            testApplication {
                configureTestApplication(mockAuthMiddleware) { healthRoutes() }

                val response = createApiClient().get(HealthResource())

                response.status shouldBe HttpStatusCode.OK
                response.body<HealthResponse>().status shouldBe "OK"
            }
        }

        "GET /api/forecasts should return 401 Unauthorized without cookies" {
            testApplication {
                configureTestApplication(mockAuthMiddleware) { forecastRoutes(useCase) }

                val response = createApiClient().get(ForecastResource())

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "GET /api/forecasts should return all seeded forecasts" {
            testApplication {
                configureTestApplication(mockAuthMiddleware) { forecastRoutes(useCase) }

                val response =
                    createApiClient().get(ForecastResource()) {
                        cookie("authToken", "valid-token")
                    }

                response.status shouldBe HttpStatusCode.OK
                val responseBody = response.body<ForecastListResponse>()
                responseBody.forecasts shouldHaveSize 2
            }
        }

        "GET /api/forecasts/{type} should filter correctly" {
            testApplication {
                configureTestApplication(mockAuthMiddleware) { forecastRoutes(useCase) }

                val resource = ForecastResource.ByType(utilityType = UtilityType.GAS)
                val response =
                    createApiClient().get(resource) {
                        cookie("authToken", "valid-token")
                    }

                response.status shouldBe HttpStatusCode.OK

                val responseBody = response.body<ForecastListResponse>()
                responseBody.forecasts shouldHaveSize 1
                responseBody.forecasts.first().utilityType shouldBe UtilityTypeDto.GAS
            }
        }
    })
