package io.energyconsumptionoptimizer.forecastservice.unit.domain.value

import arrow.core.raise.either
import io.energyconsumptionoptimizer.forecastservice.domain.DomainError
import io.energyconsumptionoptimizer.forecastservice.domain.value.ConsumptionValue
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ConsumptionValueTest :
    DescribeSpec({

        describe("ConsumptionValue.of") {

            context("given a valid amount (zero or positive)") {
                listOf(100.0, 0.0, 42.5).forEach { validAmount ->
                    it("successfully creates the value for $validAmount") {
                        either { ConsumptionValue.of(validAmount) }.shouldBeRight().amount shouldBe validAmount
                    }
                }
            }

            context("given a negative amount") {
                listOf(-1.0, -99.9).forEach { invalidAmount ->
                    it("rejects $invalidAmount with an InvalidConsumptionValue error") {
                        val error = either { ConsumptionValue.of(invalidAmount) }.shouldBeLeft()
                        error.shouldBeInstanceOf<DomainError.InvalidConsumptionValue>()
                    }
                }
            }
        }
    })
