package io.energyconsumptionoptimizer.forecastservice.unit.domain.value

import arrow.core.raise.either
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.forecastPoint
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.today
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.tomorrow
import io.energyconsumptionoptimizer.forecastservice.domain.DomainError
import io.energyconsumptionoptimizer.forecastservice.domain.value.ForecastedDataSeries
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

class ForecastedDataSeriesTest :
    DescribeSpec({

        describe("ForecastedDataSeries.of") {

            context("given a valid list of points") {
                it("creates the series, sorts points, and calculates date ranges accurately") {
                    val dayAfterTomorrow = today.plus(2, DateTimeUnit.DAY)

                    val p1 = forecastPoint(dayAfterTomorrow, 30.0)
                    val p2 = forecastPoint(today, 10.0)
                    val p3 = forecastPoint(tomorrow, 20.0)

                    val unsorted = listOf(p1, p2, p3)

                    val series = either { ForecastedDataSeries.of(unsorted) }.shouldBeRight()

                    series.points shouldBe listOf(p2, p3, p1)
                    series.startDate shouldBe today
                    series.endDate shouldBe dayAfterTomorrow

                    series.horizon shouldBe 3
                }
            }

            context("given an empty list") {
                it("rejects creation with EmptyForecastDataSeries error") {
                    val error = either { ForecastedDataSeries.of(emptyList()) }.shouldBeLeft()
                    error.shouldBeInstanceOf<DomainError.EmptyForecastDataSeries>()
                }
            }
        }
    })
