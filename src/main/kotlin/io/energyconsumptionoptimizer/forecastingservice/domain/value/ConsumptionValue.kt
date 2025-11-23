package io.energyconsumptionoptimizer.forecastingservice.domain.value

import java.util.Locale

@JvmInline
value class ConsumptionValue private constructor(
    val amount: Double,
) : Comparable<ConsumptionValue> {
    init {
        require(amount >= 0.0) { "Consumption amount cannot be negative: $amount" }
    }

    operator fun plus(other: ConsumptionValue) = ConsumptionValue(amount + other.amount)

    operator fun minus(other: ConsumptionValue): ConsumptionValue {
        val result = amount - other.amount
        require(result >= 0.0) { "Consumption value cannot be negative after subtraction: $result" }
        return ConsumptionValue(result)
    }

    operator fun times(multiplier: Double): ConsumptionValue {
        require(multiplier >= 0.0) { "Multiplier cannot be negative: $multiplier" }
        return ConsumptionValue(amount * multiplier)
    }

    operator fun div(divisor: Double): ConsumptionValue {
        require(divisor > 0.0) { "Divisor must be positive: $divisor" }
        return ConsumptionValue(amount / divisor)
    }

    override fun compareTo(other: ConsumptionValue): Int = amount.compareTo(other.amount)

    fun toFormattedString(decimals: Int = 2): String = "%.${decimals}f".format(Locale.ENGLISH, amount)

    override fun toString(): String = toFormattedString()

    companion object {
        fun of(amount: Double): ConsumptionValue = ConsumptionValue(amount)
    }
}
