package io.energyconsumptionoptimizer.forecastingservice.domain.value

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe

class ConsumptionValueTest :
    BehaviorSpec({
        Given("a consumption value creation") {
            When("attempting to create with a negative value") {
                Then("it should reject with an error") {
                    shouldThrow<IllegalArgumentException> {
                        ConsumptionValue.of(-10.0)
                    }.message shouldBe "Consumption amount cannot be negative: -10.0"
                }
            }
        }

        Given("two consumption values for subtraction") {
            val small = ConsumptionValue.of(30.0)
            val large = ConsumptionValue.of(50.0)

            When("subtracting a larger value from a smaller one") {
                Then("it should reject with an error") {
                    shouldThrow<IllegalArgumentException> {
                        small - large
                    }.message shouldBe "Consumption value cannot be negative after subtraction: -20.0"
                }
            }
        }

        Given("a consumption value for multiplication") {
            val value = ConsumptionValue.of(10.0)

            When("multiplying by a negative number") {
                Then("it should reject with an error") {
                    shouldThrow<IllegalArgumentException> {
                        value * -1.0
                    }.message shouldBe "Multiplier cannot be negative: -1.0"
                }
            }
        }

        Given("a consumption value for division") {
            val value = ConsumptionValue.of(10.0)

            When("dividing by zero") {
                Then("it should reject with an error") {
                    shouldThrow<IllegalArgumentException> {
                        value / 0.0
                    }.message shouldBe "Divisor must be positive: 0.0"
                }
            }
        }

        Given("two valid consumption values") {
            val v1 = ConsumptionValue.of(100.0)
            val v2 = ConsumptionValue.of(50.0)

            When("performing addition") {
                Then("it should return the sum") {
                    (v1 + v2).amount shouldBe 150.0
                }
            }

            When("performing subtraction") {
                Then("it should return the difference") {
                    (v1 - v2).amount shouldBe 50.0
                }
            }

            When("multiplying by a scalar") {
                Then("it should return the product") {
                    (v1 * 2.0).amount shouldBe 200.0
                }
            }

            When("dividing by a scalar") {
                Then("it should return the quotient") {
                    (v1 / 2.0).amount shouldBe 50.0
                }
            }
        }

        Given("consumption values for comparison") {
            val small = ConsumptionValue.of(10.0)
            val large = ConsumptionValue.of(100.0)

            When("comparing them") {
                Then("it should order them correctly") {
                    small shouldBeLessThan large
                    large shouldBeGreaterThan small
                }
            }
        }

        Given("a consumption value with decimals") {
            val value = ConsumptionValue.of(12.3456)

            When("formatting with specified precision") {
                Then("it should round correctly") {
                    value.toFormattedString(2) shouldBe "12.35"
                }
            }
        }
    })
