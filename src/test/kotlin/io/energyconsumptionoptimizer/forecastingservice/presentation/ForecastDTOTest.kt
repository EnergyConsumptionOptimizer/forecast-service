package io.energyconsumptionoptimizer.forecastingservice.presentation

import io.energyconsumptionoptimizer.forecastingservice.domain.ForecastedConsumption
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ConsumptionValue
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastedDataPoint
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType
import io.energyconsumptionoptimizer.forecastingservice.presentation.dto.UtilityTypeDto
import io.energyconsumptionoptimizer.forecastingservice.presentation.dto.toDTO
import io.energyconsumptionoptimizer.forecastingservice.presentation.dto.toListDTO
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class ForecastDTOTest :
    BehaviorSpec({

        val json = Json { ignoreUnknownKeys = true }

        val currentInstant = Clock.System.now()
        val today = currentInstant.toLocalDateTime(TimeZone.UTC).date
        val tomorrow = today.plus(1, DateTimeUnit.DAY)

        Given("a domain forecast object") {

            val domainForecast =
                ForecastedConsumption.create(
                    utilityType = UtilityType.ELECTRICITY,
                    forecastedDataPoints =
                        listOf(
                            ForecastedDataPoint(today, ConsumptionValue.of(10.0)),
                            ForecastedDataPoint(tomorrow, ConsumptionValue.of(20.0)),
                        ),
                    computedAt = currentInstant,
                )

            When("mapping to DTO") {
                val dto = domainForecast.toDTO()

                Then("all fields should be mapped correctly") {
                    dto.id shouldBe domainForecast.id.value.toString()
                    dto.utilityType shouldBe UtilityTypeDto.ELECTRICITY
                    dto.computedAt shouldBe currentInstant
                    dto.startDate shouldBe today
                    dto.endDate shouldBe tomorrow
                    dto.durationDays shouldBe 2
                }

                Then("data points should be mapped accurately") {
                    dto.dataPoints.size shouldBe 2
                    dto.dataPoints[0].predictedConsumption shouldBe 10.0
                    dto.dataPoints[0].date shouldBe today
                }

                Then("it should serialize to JSON correctly using kotlinx native support") {
                    val jsonString = json.encodeToString(dto)

                    jsonString shouldContain today.toString()
                    jsonString shouldContain currentInstant.toString()
                    jsonString shouldContain "ELECTRICITY"
                }
            }
        }

        Given("a list of domain forecasts") {
            val list =
                listOf(
                    ForecastedConsumption.create(
                        UtilityType.GAS,
                        listOf(ForecastedDataPoint(today, ConsumptionValue.of(5.0))),
                    ),
                    ForecastedConsumption.create(
                        UtilityType.WATER,
                        listOf(ForecastedDataPoint(today, ConsumptionValue.of(8.0))),
                    ),
                )

            When("mapping using toListDTO()") {
                val listDto = list.toListDTO()

                Then("it should contain the correct count and items") {
                    listDto.count shouldBe 2
                    listDto.forecasts[0].utilityType shouldBe UtilityTypeDto.GAS
                    listDto.forecasts[1].utilityType shouldBe UtilityTypeDto.WATER
                }
            }
        }
    })
