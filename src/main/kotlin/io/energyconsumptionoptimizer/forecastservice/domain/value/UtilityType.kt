package io.energyconsumptionoptimizer.forecastservice.domain.value

import arrow.core.raise.Raise
import arrow.core.raise.ensureNotNull
import io.energyconsumptionoptimizer.forecastservice.domain.DomainError

enum class UtilityType {
    ELECTRICITY,
    GAS,
    WATER,
    ;

    companion object {
        context(raise: Raise<DomainError.UnknownUtilityType>)
        fun of(value: String): UtilityType {
            val trimmed = value.trim().uppercase()
            return raise.ensureNotNull(
                entries.firstOrNull { it.name == trimmed },
            ) {
                DomainError.UnknownUtilityType(value)
            }
        }
    }
}
