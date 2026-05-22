package io.energyconsumptionoptimizer.forecastservice.unit.domain.value

import arrow.core.raise.either
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.historicalPoint
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.today
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.yesterday
import io.energyconsumptionoptimizer.forecastservice.domain.DomainError
import io.energyconsumptionoptimizer.forecastservice.domain.value.HistoricalDataSeries
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus

class HistoricalDataSeriesTest :
    DescribeSpec({

        describe("HistoricalDataSeries.of") {

            context("given a valid list of points") {
                it("creates the series, sorts points chronologically, and exposes size") {
                    val dayBeforeYesterday = today.minus(2, DateTimeUnit.DAY)
                    val p1 = historicalPoint(today, 10.0)
                    val p2 = historicalPoint(dayBeforeYesterday, 20.0)
                    val p3 = historicalPoint(yesterday, 30.0)

                    val unsorted = listOf(p1, p2, p3)

                    val series = either { HistoricalDataSeries.of(unsorted) }.shouldBeRight()

                    series.points shouldBe listOf(p2, p3, p1)
                    series.size shouldBe 3
                }
            }

            context("given an empty list") {
                it("rejects creation with EmptyHistoricalDataSeries error") {
                    val error = either { HistoricalDataSeries.of(emptyList()) }.shouldBeLeft()
                    error.shouldBeInstanceOf<DomainError.EmptyHistoricalDataSeries>()
                }
            }
        }
    })
