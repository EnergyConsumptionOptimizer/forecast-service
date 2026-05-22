package io.energyconsumptionoptimizer.forecastservice.integration.persistence

import io.energyconsumptionoptimizer.forecastservice.DomainFixtures
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import io.energyconsumptionoptimizer.forecastservice.infrastructure.persistence.mongo.MongoForecastRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.TestAbortedException
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.litote.kmongo.coroutine.CoroutineClient

class MongoForecastRepositoryIntegrationTest :
    DescribeSpec({

        val databaseName = "test-forecast-repo"
        lateinit var repository: MongoForecastRepository
        var client: CoroutineClient? = null

        fun needsMongo() {
            if (!MongoSetup.available) throw TestAbortedException("Embedded MongoDB not available")
        }

        beforeSpec {
            MongoSetup.start()
            if (MongoSetup.available) {
                val c = MongoSetup.createClient()
                client = c
                repository = MongoForecastRepository(c, databaseName)
            }
        }

        afterSpec {
            client?.let { c ->
                MongoSetup.clearDatabase(c, databaseName)
                c.close()
            }
            MongoSetup.stop()
        }

        beforeEach { client?.let { c -> MongoSetup.clearDatabase(c, databaseName) } }

        describe("saving a forecast") {

            it("should persist and make it retrievable by utility type") {
                needsMongo()

                val forecast = DomainFixtures.aForecast(UtilityType.ELECTRICITY)

                repository.save(forecast)

                val retrieved = repository.findByUtility(UtilityType.ELECTRICITY)
                retrieved.shouldNotBeNull()
                retrieved.utilityType shouldBe UtilityType.ELECTRICITY
                retrieved.series.points shouldHaveSize 2
            }

            it("should upsert when a forecast already exists for the same utility") {
                needsMongo()

                val first = DomainFixtures.aForecast(UtilityType.GAS)
                val second = DomainFixtures.aForecast(UtilityType.GAS)

                repository.save(first)
                repository.save(second)

                repository.findAll() shouldHaveSize 1
            }
        }

        describe("retrieving all forecasts") {

            it("should return every stored forecast") {
                needsMongo()

                repository.save(DomainFixtures.aForecast(UtilityType.ELECTRICITY))
                repository.save(DomainFixtures.aForecast(UtilityType.GAS))

                repository.findAll() shouldHaveSize 2
            }

            it("should return an empty list when nothing is stored") {
                needsMongo()

                repository.findAll() shouldHaveSize 0
            }
        }

        describe("finding a forecast by utility type") {

            it("should return the matching forecast") {
                needsMongo()

                repository.save(DomainFixtures.aForecast(UtilityType.WATER))

                val retrieved = repository.findByUtility(UtilityType.WATER)
                retrieved.shouldNotBeNull()
                retrieved.utilityType shouldBe UtilityType.WATER
            }

            it("should return null when no forecast matches") {
                needsMongo()

                repository.findByUtility(UtilityType.ELECTRICITY).shouldBeNull()
            }
        }

        describe("removing a forecast") {

            it("should delete it so a subsequent retrieval returns null") {
                needsMongo()

                val forecast = DomainFixtures.aForecast(UtilityType.ELECTRICITY)
                repository.save(forecast)

                repository.remove(forecast)

                repository.findByUtility(UtilityType.ELECTRICITY).shouldBeNull()
            }
        }
    })
