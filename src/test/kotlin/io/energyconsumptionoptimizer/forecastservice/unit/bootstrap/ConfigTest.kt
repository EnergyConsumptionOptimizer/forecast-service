package io.energyconsumptionoptimizer.forecastservice.unit.bootstrap

import io.energyconsumptionoptimizer.forecastservice.bootstrap.Config
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ConfigTest :
    DescribeSpec({
        describe("Config.fromEnv") {
            it("should create config with defaults when no env vars are set") {
                val config = Config.fromEnv()

                config.port shouldBe 3000
                config.mongoUri shouldBe "mongodb://localhost:27017"
                config.mongoDatabase shouldBe "forecast"
                config.monitoringServiceUrl shouldBe "http://monitoring-service:3000"
                config.schedulerHour shouldBe 0
                config.schedulerMinute shouldBe 0
            }
        }
    })
