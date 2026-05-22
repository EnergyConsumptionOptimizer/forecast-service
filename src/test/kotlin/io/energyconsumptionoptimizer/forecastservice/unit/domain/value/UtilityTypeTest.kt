package io.energyconsumptionoptimizer.forecastservice.unit.domain.value

import arrow.core.raise.either
import io.energyconsumptionoptimizer.forecastservice.domain.DomainError
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class UtilityTypeTest :
    DescribeSpec({

        describe("UtilityType.of") {
            context("given valid utility strings") {
                UtilityType.entries.forEach { utility ->
                    it("successfully parses the exact name: ${utility.name}") {
                        either { UtilityType.of(utility.name) }.shouldBeRight() shouldBe utility
                    }
                }

                listOf(
                    " electricity " to UtilityType.ELECTRICITY,
                    "gAs" to UtilityType.GAS,
                    "water" to UtilityType.WATER,
                ).forEach { (input, expected) ->
                    it("parses case-insensitively and trims whitespace for '$input'") {
                        either { UtilityType.of(input) }.shouldBeRight() shouldBe expected
                    }
                }
            }

            context("given an unknown or invalid utility string") {
                it("rejects the value with an UnknownUtilityType error") {
                    val invalidInput = "INVALID_UTILITY"

                    val error = either { UtilityType.of(invalidInput) }.shouldBeLeft()

                    error.shouldBeInstanceOf<DomainError.UnknownUtilityType>()
                    error.value shouldBe invalidInput
                }
            }
        }
    })
