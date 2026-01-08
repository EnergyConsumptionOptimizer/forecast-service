package io.energyconsumptionoptimizer.forecastingservice.domain.value

import java.util.UUID

/**
 * Strongly-typed identifier for forecasts.
 */
@JvmInline
value class ForecastId private constructor(
    val value: UUID,
) {
    override fun toString(): String = value.toString()

    companion object {
        /**
         * Generate a new random `ForecastId`.
         */
        fun generate(): ForecastId = ForecastId(UUID.randomUUID())

        /**
         * Parse a `ForecastId` from its string representation.
         *
         * @param value String representation of the UUID.
         * @return Parsed [ForecastId].
         * @throws IllegalArgumentException When the provided string is not a valid UUID.
         */
        fun from(value: String): ForecastId {
            val parsed = runCatching { UUID.fromString(value) }.getOrNull()
            return requireNotNull(parsed) { "Invalid ForecastId format: '$value'" }.let(::ForecastId)
        }
    }
}
