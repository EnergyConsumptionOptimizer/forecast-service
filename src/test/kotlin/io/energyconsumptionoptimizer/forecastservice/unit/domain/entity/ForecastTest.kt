package io.energyconsumptionoptimizer.forecastservice.unit.domain.entity

import arrow.core.raise.either
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.forecastPoint
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.today
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.tomorrow
import io.energyconsumptionoptimizer.forecastservice.domain.entity.Forecast
import io.energyconsumptionoptimizer.forecastservice.domain.event.ForecastComputedEvent
import io.energyconsumptionoptimizer.forecastservice.domain.value.ForecastedDataSeries
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlin.time.Clock

class ForecastTest :
    DescribeSpec({

        val anyUtilityType = UtilityType.ELECTRICITY

        val dp1 = forecastPoint(today, 10.0)
        val dp2 = forecastPoint(tomorrow, 20.0)
        val dp3 = forecastPoint(today.plus(2, DateTimeUnit.DAY), 30.0)

        val initialSeries = either { ForecastedDataSeries.of(listOf(dp1)) }.shouldBeRight()
        val newSeries = either { ForecastedDataSeries.of(listOf(dp2, dp3)) }.shouldBeRight()

        describe("Forecast Aggregate") {

            describe("factory method 'create'") {
                context("given valid data with explicit time") {
                    it("creates the forecast and uses the provided time") {
                        val explicitTime = Clock.System.now()

                        val forecast = Forecast.create(anyUtilityType, initialSeries, at = explicitTime)

                        forecast.computedAt shouldBe explicitTime
                        forecast.series shouldBe initialSeries
                    }
                }

                context("given valid data with default time") {
                    it("creates the forecast using the real system time") {
                        val beforeExecution = Clock.System.now()

                        val forecast = Forecast.create(anyUtilityType, initialSeries)

                        forecast.computedAt shouldBeGreaterThanOrEqualTo beforeExecution
                    }
                }
            }

            describe("method 'recompute'") {
                it("updates the forecast, sets new time, and raises a Domain Event") {
                    val initialTime = Clock.System.now()
                    val forecast = Forecast.create(anyUtilityType, initialSeries, initialTime)
                    forecast.pullDomainEvents()

                    val laterTime = Clock.System.now().plus(1, DateTimeUnit.HOUR, TimeZone.UTC)

                    forecast.recompute(newSeries, laterTime)

                    forecast.computedAt shouldBe laterTime
                    forecast.series shouldBe newSeries

                    val events = forecast.pullDomainEvents()
                    val event = events.first() as ForecastComputedEvent
                    event.dataPoints shouldBe newSeries
                    event.occurredAt shouldBe laterTime
                }
            }

            describe("factory method 'restore'") {
                it("restores the aggregate without raising any Domain Events") {
                    val restoreTime = Clock.System.now()
                    val forecast = Forecast.restore(anyUtilityType, initialSeries, restoreTime)

                    forecast.series shouldBe initialSeries
                    forecast.pullDomainEvents().shouldBeEmpty()
                }
            }

            describe("Entity Equality (equals / hashCode)") {
                it("considers forecasts equal if they have the same UtilityType") {
                    val time = Clock.System.now()
                    val forecastA = Forecast.restore(UtilityType.ELECTRICITY, initialSeries, time)
                    val forecastB = Forecast.restore(UtilityType.ELECTRICITY, newSeries, time)
                    val forecastC = Forecast.restore(UtilityType.GAS, initialSeries, time)

                    forecastA shouldBe forecastB
                    forecastA.hashCode() shouldBe forecastB.hashCode()
                    forecastA shouldNotBe forecastC
                }
            }
        }
    })
