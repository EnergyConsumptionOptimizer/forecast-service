package io.energyconsumptionoptimizer.forecastservice.integration.persistence

import arrow.core.raise.either
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import io.energyconsumptionoptimizer.forecastservice.infrastructure.events.MongoOutboxEventPublisher
import io.energyconsumptionoptimizer.forecastservice.infrastructure.persistence.mongo.MongoForecastRepository
import io.energyconsumptionoptimizer.forecastservice.infrastructure.persistence.mongo.MongoUnitOfWork
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.TestAbortedException
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.litote.kmongo.coroutine.CoroutineClient

class OutboxPatternIntegrationTest :
    DescribeSpec({

        val databaseName = "test-forecast-outbox"
        lateinit var repository: MongoForecastRepository
        lateinit var unitOfWork: MongoUnitOfWork
        lateinit var eventPublisher: MongoOutboxEventPublisher
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
                unitOfWork = MongoUnitOfWork(c)
                eventPublisher = MongoOutboxEventPublisher(c, databaseName)
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

        describe("atomic transaction with outbox events") {

            it("should persist both the forecast and its domain events together") {
                needsMongo()

                val forecast = DomainFixtures.aForecast(UtilityType.ELECTRICITY)
                val events = forecast.pullDomainEvents()
                events shouldHaveSize 1

                either {
                    unitOfWork.executeTransactionally {
                        repository.save(forecast)
                        events.forEach { eventPublisher.publish(it) }
                    }
                }.shouldBeRight()

                val retrieved = repository.findByUtility(UtilityType.ELECTRICITY)
                retrieved.shouldNotBeNull()
                retrieved.utilityType shouldBe UtilityType.ELECTRICITY
            }

            it("should roll back the forecast when a failure occurs during the transaction") {
                needsMongo()

                either {
                    unitOfWork.executeTransactionally {
                        repository.save(DomainFixtures.aForecast(UtilityType.GAS))
                        throw IllegalStateException("Simulated failure")
                    }
                }.shouldBeLeft()

                repository.findByUtility(UtilityType.GAS).shouldBeNull()
            }

            it("should allow independent commits for separate transactions") {
                needsMongo()

                val electricity = DomainFixtures.aForecast(UtilityType.ELECTRICITY)
                val gas = DomainFixtures.aForecast(UtilityType.GAS)

                either {
                    unitOfWork.executeTransactionally {
                        repository.save(electricity)
                        electricity.pullDomainEvents().forEach { eventPublisher.publish(it) }
                    }
                }.shouldBeRight()

                either {
                    unitOfWork.executeTransactionally {
                        repository.save(gas)
                        gas.pullDomainEvents().forEach { eventPublisher.publish(it) }
                    }
                }.shouldBeRight()

                repository.findAll() shouldHaveSize 2
            }
        }
    })
