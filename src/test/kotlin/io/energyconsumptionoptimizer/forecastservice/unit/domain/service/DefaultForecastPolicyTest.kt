package io.energyconsumptionoptimizer.forecastservice.unit.domain.service

import arrow.core.raise.either
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.forecastPoint
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.today
import io.energyconsumptionoptimizer.forecastservice.domain.DomainError
import io.energyconsumptionoptimizer.forecastservice.domain.service.DefaultForecastPolicy
import io.energyconsumptionoptimizer.forecastservice.domain.value.ForecastedDataSeries
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

class DefaultForecastPolicyTest :
    DescribeSpec({

        val anyUtility = UtilityType.ELECTRICITY

        describe("DefaultForecastPolicy") {

            describe("maxHorizon") {
                it("returns exactly 100 days for all known UtilityTypes") {
                    UtilityType.entries.forEach { type ->
                        DefaultForecastPolicy.maxHorizon(type) shouldBe 100
                    }
                }
            }

            describe("validate") {

                context("given a series within the maximum allowed horizon") {
                    it("validates successfully") {
                        val points =
                            listOf(
                                forecastPoint(today, 10.0),
                                forecastPoint(today.plus(99, DateTimeUnit.DAY), 20.0),
                            )

                        val validSeries = either { ForecastedDataSeries.of(points) }.shouldBeRight()

                        either { DefaultForecastPolicy.validate(anyUtility, validSeries) }.shouldBeRight()
                    }
                }

                context("given a series exceeding the maximum allowed horizon") {
                    it("rejects with ForecastHorizonExceeded error") {
                        val points =
                            listOf(
                                forecastPoint(today, 10.0),
                                forecastPoint(today.plus(100, DateTimeUnit.DAY), 20.0),
                            )
                        val exceedingSeries = either { ForecastedDataSeries.of(points) }.shouldBeRight()

                        val error = either { DefaultForecastPolicy.validate(anyUtility, exceedingSeries) }.shouldBeLeft()

                        error.shouldBeInstanceOf<DomainError.ForecastHorizonExceeded>()
                        error.requestedDays shouldBe 101
                        error.maxAllowed shouldBe 100
                    }
                }
            }
        }
    })
