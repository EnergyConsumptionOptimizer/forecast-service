package io.energyconsumptionoptimizer.forecastservice.unit.domain.value

import arrow.core.raise.either
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.cv
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.today
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.tomorrow
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.yesterday
import io.energyconsumptionoptimizer.forecastservice.domain.DomainError
import io.energyconsumptionoptimizer.forecastservice.domain.value.HistoricalDataPoint
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class HistoricalDataPointTest :
    DescribeSpec({

        describe("HistoricalDataPoint") {

            describe("factory method 'of'") {
                context("given a valid past date or today") {
                    it("successfully creates the value") {
                        val p1 = either { HistoricalDataPoint.of(yesterday, cv(100.0)) }.shouldBeRight()
                        p1.date shouldBe yesterday
                        p1.value.amount shouldBe 100.0

                        val p2 = either { HistoricalDataPoint.of(today, cv(100.0)) }.shouldBeRight()
                        p2.date shouldBe today
                    }
                }

                context("given a future date") {
                    it("rejects the date with a FutureHistoricalDate error") {
                        val error = either { HistoricalDataPoint.of(tomorrow, cv(100.0)) }.shouldBeLeft()

                        error.shouldBeInstanceOf<DomainError.FutureHistoricalDate>()
                        error.date shouldBe tomorrow
                    }
                }
            }

            describe("structural behavior") {
                it("should be comparable by date") {
                    val p1 = either { HistoricalDataPoint.of(yesterday, cv(100.0)) }.shouldBeRight()
                    val p2 = either { HistoricalDataPoint.of(today, cv(100.0)) }.shouldBeRight()

                    p1 shouldBeLessThan p2
                    p2 shouldBeGreaterThan p1
                }

                it("should be equal when dates and values are the same") {
                    val p1 = either { HistoricalDataPoint.of(yesterday, cv(100.0)) }.shouldBeRight()
                    val p2 = either { HistoricalDataPoint.of(yesterday, cv(100.0)) }.shouldBeRight()

                    p1 shouldBe p2
                }
            }
        }
    })
