package io.energyconsumptionoptimizer.forecastservice.unit.domain.value

import arrow.core.raise.either
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.cv
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.today
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.tomorrow
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.yesterday
import io.energyconsumptionoptimizer.forecastservice.domain.DomainError
import io.energyconsumptionoptimizer.forecastservice.domain.value.ForecastedDataPoint
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ForecastedDataPointTest :
    DescribeSpec({

        describe("ForecastedDataPoint") {

            describe("factory method 'of'") {
                context("given a valid future date") {
                    it("successfully creates the value") {
                        val p1 = either { ForecastedDataPoint.of(tomorrow, cv(10.0)) }.shouldBeRight()

                        p1.date shouldBe tomorrow
                        p1.value.amount shouldBe 10.0
                    }
                }

                context("given a past date") {
                    it("rejects the date with a PastForecastDate error") {
                        val error = either { ForecastedDataPoint.of(yesterday, cv(10.0)) }.shouldBeLeft()

                        error.shouldBeInstanceOf<DomainError.PastForecastDate>()
                        error.date shouldBe yesterday
                    }
                }
            }

            describe("structural behavior") {
                it("should be equal when dates and values are the same") {
                    val p1 = either { ForecastedDataPoint.of(tomorrow, cv(10.0)) }.shouldBeRight()
                    val p2 = either { ForecastedDataPoint.of(tomorrow, cv(10.0)) }.shouldBeRight()

                    p1 shouldBe p2
                }

                it("should be comparable by date") {
                    val p1 = either { ForecastedDataPoint.of(today, cv(10.0)) }.shouldBeRight()
                    val p2 = either { ForecastedDataPoint.of(tomorrow, cv(20.0)) }.shouldBeRight()

                    p1 shouldBeLessThan p2
                    p2 shouldBeGreaterThan p1
                }
            }
        }
    })
