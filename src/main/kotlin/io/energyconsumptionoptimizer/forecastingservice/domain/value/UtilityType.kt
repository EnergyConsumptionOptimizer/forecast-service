package io.energyconsumptionoptimizer.forecastingservice.domain.value

import io.energyconsumptionoptimizer.forecastingservice.domain.error.UnknownUtilityTypeException

/**
 * Supported utility types with their measurement unit.
 */
enum class UtilityType(
    val unit: String,
) {
    ELECTRICITY("Wh"),
    GAS("m³"),
    WATER("m³"),
    ;

    companion object {
        /**
         * Parse a `UtilityType` from a case-insensitive string.
         *
         * @param value Input string to parse.
         * @return Parsed [UtilityType].
         * @throws UnknownUtilityTypeException When the value is not recognized.
         */
        fun fromString(value: String): UtilityType =
            runCatching { valueOf(value.trim().uppercase()) }
                .getOrElse { throw UnknownUtilityTypeException(value) }
    }
}
