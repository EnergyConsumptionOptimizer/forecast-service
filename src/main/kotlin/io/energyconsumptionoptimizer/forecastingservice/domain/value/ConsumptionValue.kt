package io.energyconsumptionoptimizer.forecastingservice.domain.value

import java.util.Locale

/**
 * Value object representing a non-negative consumption amount.
 *
 * Provides arithmetic operators and formatting helpers.
 */
@JvmInline
value class ConsumptionValue private constructor(
    val amount: Double,
) : Comparable<ConsumptionValue> {
    init {
        require(amount >= 0.0) { "Consumption amount cannot be negative: $amount" }
    }

    /**
     * Add two consumption values.
     *
     * @param other The value to add.
     * @return New [ConsumptionValue] representing the sum (non-negative).
     */
    operator fun plus(other: ConsumptionValue) = ConsumptionValue(amount + other.amount)

    /**
     * Subtract another consumption value; result must remain non-negative.
     *
     * @param other The value to subtract.
     * @return New [ConsumptionValue] representing the difference.
     * @throws IllegalArgumentException When the subtraction would produce a negative amount.
     */
    operator fun minus(other: ConsumptionValue): ConsumptionValue {
        val result = amount - other.amount
        require(result >= 0.0) { "Consumption value cannot be negative after subtraction: $result" }
        return ConsumptionValue(result)
    }

    /**
     * Multiply the consumption by a non-negative scalar.
     *
     * @param multiplier Non-negative scalar to multiply by.
     * @return New [ConsumptionValue] scaled by `multiplier`.
     * @throws IllegalArgumentException When `multiplier` is negative.
     */
    operator fun times(multiplier: Double): ConsumptionValue {
        require(multiplier >= 0.0) { "Multiplier cannot be negative: $multiplier" }
        return ConsumptionValue(amount * multiplier)
    }

    /**
     * Divide the consumption by a positive scalar.
     *
     * @param divisor Positive scalar divisor.
     * @return New [ConsumptionValue] after division.
     * @throws IllegalArgumentException When `divisor` is not positive.
     */
    operator fun div(divisor: Double): ConsumptionValue {
        require(divisor > 0.0) { "Divisor must be positive: $divisor" }
        return ConsumptionValue(amount / divisor)
    }

    override fun compareTo(other: ConsumptionValue): Int = amount.compareTo(other.amount)

    /**
     * Format the value as a string using a fixed number of decimal places.
     *
     * Defaults to two decimals and uses the English locale for the decimal separator.
     *
     * @param decimals Number of decimal places to include (non-negative).
     * @return Formatted string representation of the amount.
     */
    fun toFormattedString(decimals: Int = 2): String = "%.${decimals}f".format(Locale.ENGLISH, amount)

    override fun toString(): String = toFormattedString()

    companion object {
        /**
         * Create a [ConsumptionValue] from a raw double.
         *
         * @param amount Raw consumption amount (must be non-negative).
         * @return New [ConsumptionValue] wrapping `amount`.
         * @throws IllegalArgumentException When `amount` is negative.
         */
        fun of(amount: Double): ConsumptionValue = ConsumptionValue(amount)
    }
}
