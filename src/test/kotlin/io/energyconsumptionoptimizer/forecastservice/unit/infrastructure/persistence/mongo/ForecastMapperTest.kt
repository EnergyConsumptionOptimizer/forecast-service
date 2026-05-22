package io.energyconsumptionoptimizer.forecastservice.unit.infrastructure.persistence.mongo

import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.aForecast
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import io.energyconsumptionoptimizer.forecastservice.infrastructure.persistence.mongo.DataPointDocument
import io.energyconsumptionoptimizer.forecastservice.infrastructure.persistence.mongo.ForecastDocument
import io.energyconsumptionoptimizer.forecastservice.infrastructure.persistence.mongo.ForecastMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class ForecastMapperTest :
    DescribeSpec({

        val now = Clock.System.now()
        val futureDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(1, DateTimeUnit.DAY)

        describe("ForecastMapper") {

            describe("toDocument") {

                context("given a valid Forecast") {
                    it("should map to a ForecastDocument with matching fields") {
                        val forecast = aForecast(UtilityType.ELECTRICITY)

                        val doc = ForecastMapper.toDocument(forecast)

                        doc.utilityType shouldBe UtilityType.ELECTRICITY.name
                        doc.dataPoints shouldHaveSize forecast.series.points.size
                    }
                }
            }

            describe("toDomain") {

                context("given a valid ForecastDocument") {
                    it("should map to a Forecast with reconstructed series and values") {
                        val doc =
                            ForecastDocument(
                                utilityType = "ELECTRICITY",
                                dataPoints =
                                    listOf(
                                        DataPointDocument(date = futureDate, value = 150.0),
                                    ),
                                computedAt = now.toEpochMilliseconds(),
                            )

                        val forecast = ForecastMapper.toDomain(doc).shouldNotBeNull()
                        forecast.utilityType shouldBe UtilityType.ELECTRICITY
                        forecast.series.points shouldHaveSize 1
                        forecast.series.points
                            .first()
                            .value.amount shouldBe 150.0
                    }
                }

                context("given a document with an unknown utility type") {
                    it("should return null") {
                        val doc =
                            ForecastDocument(
                                utilityType = "INVALID",
                                dataPoints =
                                    listOf(
                                        DataPointDocument(date = futureDate, value = 150.0),
                                    ),
                                computedAt = now.toEpochMilliseconds(),
                            )

                        ForecastMapper.toDomain(doc).shouldBeNull()
                    }
                }

                context("given a document with a negative consumption value") {
                    it("should return null") {
                        val doc =
                            ForecastDocument(
                                utilityType = "ELECTRICITY",
                                dataPoints =
                                    listOf(
                                        DataPointDocument(date = futureDate, value = -50.0),
                                    ),
                                computedAt = now.toEpochMilliseconds(),
                            )

                        ForecastMapper.toDomain(doc).shouldBeNull()
                    }
                }
            }
        }
    })
