package io.energyconsumptionoptimizer.forecastingservice.domain.value

import java.util.UUID

@JvmInline
value class ForecastId private constructor(
    val value: UUID,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): ForecastId = ForecastId(UUID.randomUUID())

        fun from(value: String): ForecastId {
            val parsed = runCatching { UUID.fromString(value) }.getOrNull()
            return requireNotNull(parsed) { "Invalid ForecastId format: '$value'" }.let(::ForecastId)
        }
    }
}
