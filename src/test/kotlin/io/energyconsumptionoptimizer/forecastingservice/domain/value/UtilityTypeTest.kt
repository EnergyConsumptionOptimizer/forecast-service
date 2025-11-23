package io.energyconsumptionoptimizer.forecastingservice.domain.value

import io.energyconsumptionoptimizer.forecastingservice.domain.error.UnknownUtilityTypeException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class UtilityTypeTest :
    BehaviorSpec({
        Given("utility type values") {
            When("converting to string") {
                Then("it should return uppercase name") {
                    UtilityType.ELECTRICITY.toString() shouldBe "ELECTRICITY"
                    UtilityType.GAS.toString() shouldBe "GAS"
                }
            }
        }

        Given("various string representations of utility types") {
            When("parsing case-insensitive names") {
                Then("it should recognize all variations") {
                    UtilityType.fromString("electricity") shouldBe UtilityType.ELECTRICITY
                    UtilityType.fromString("GAS") shouldBe UtilityType.GAS
                    UtilityType.fromString("Water") shouldBe UtilityType.WATER
                }
            }

            When("parsing an unknown type") {
                Then("it should reject with an error") {
                    val ex =
                        shouldThrow<UnknownUtilityTypeException> {
                            UtilityType.fromString("steam")
                        }
                    ex.message shouldBe "Unknown utility type: 'steam'"
                }
            }
        }

        Given("different utility types") {
            When("requesting their measurement units") {
                Then("it should provide the correct unit for each type") {
                    UtilityType.ELECTRICITY.unit shouldBe "Wh"
                    UtilityType.GAS.unit shouldBe "m³"
                    UtilityType.WATER.unit shouldBe "m³"
                }
            }
        }
    })
