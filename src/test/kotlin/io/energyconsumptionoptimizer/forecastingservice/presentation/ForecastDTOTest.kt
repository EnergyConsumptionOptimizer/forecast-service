package io.energyconsumptionoptimizer.forecastingservice.presentation

import io.energyconsumptionoptimizer.forecastingservice.domain.ForecastedConsumption
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ConsumptionValue
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastedDataPoint
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType
import io.energyconsumptionoptimizer.forecastingservice.presentation.dto.UtilityTypeDto
import io.energyconsumptionoptimizer.forecastingservice.presentation.dto.toDTO
import io.energyconsumptionoptimizer.forecastingservice.presentation.dto.toListDTO
import io.energyconsumptionoptimizer.forecastingservice.presentation.serializers.InstantSerializer
import io.energyconsumptionoptimizer.forecastingservice.presentation.serializers.LocalDateSerializer
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

class ForecastDTOTest :
    BehaviorSpec({

        val json = Json { ignoreUnknownKeys = true }

        Given("a domain forecast object") {
            val today = LocalDate.now()
            val currentInstant = Instant.now()

            val domainForecast =
                ForecastedConsumption.create(
                    utilityType = UtilityType.ELECTRICITY,
                    forecastedDataPoints =
                        listOf(
                            ForecastedDataPoint(today, ConsumptionValue.of(10.0)),
                            ForecastedDataPoint(today.plusDays(1), ConsumptionValue.of(20.0)),
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
                    dto.endDate shouldBe today.plusDays(1)
                    dto.durationDays shouldBe 2
                }

                Then("data points should be mapped accurately") {
                    dto.dataPoints.size shouldBe 2
                    dto.dataPoints[0].predictedConsumption shouldBe 10.0
                    dto.dataPoints[0].date shouldBe today
                }
            }
        }

        Given("a list of domain forecasts") {
            val today = LocalDate.now()
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

        Given("the InstantSerializer") {
            @Serializable
            data class InstantWrapper(
                @Serializable(with = InstantSerializer::class) val time: Instant,
            )

            val now = Instant.now()
            val wrapper = InstantWrapper(now)

            When("serializing to JSON") {
                val jsonString = json.encodeToString(InstantWrapper.serializer(), wrapper)

                Then("it should produce a standard ISO-8601 string") {
                    jsonString shouldBe """{"time":"$now"}"""
                }
            }

            When("deserializing from JSON") {
                val jsonString = """{"time":"$now"}"""
                val result = json.decodeFromString(InstantWrapper.serializer(), jsonString)

                Then("it should return the correct Instant object") {
                    result.time shouldBe now
                }
            }
        }

        Given("the LocalDateSerializer") {
            @Serializable
            data class DateWrapper(
                @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
            )

            val today = LocalDate.now()
            val wrapper = DateWrapper(today)

            When("serializing to JSON") {
                val jsonString = json.encodeToString(DateWrapper.serializer(), wrapper)

                Then("it should produce a simple date string") {
                    jsonString shouldBe """{"date":"$today"}"""
                }
            }

            When("deserializing from JSON") {
                val jsonString = """{"date":"$today"}"""
                val result = json.decodeFromString(DateWrapper.serializer(), jsonString)

                Then("it should return the correct LocalDate object") {
                    result.date shouldBe today
                }
            }
        }
    })
