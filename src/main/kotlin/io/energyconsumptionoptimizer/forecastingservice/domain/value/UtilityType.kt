package io.energyconsumptionoptimizer.forecastingservice.domain.value

import io.energyconsumptionoptimizer.forecastingservice.domain.error.UnknownUtilityTypeException

enum class UtilityType(
    val unit: String,
) {
    ELECTRICITY("Wh"),
    GAS("m³"),
    WATER("m³"),
    ;

    companion object {
        fun fromString(value: String): UtilityType =
            runCatching { valueOf(value.trim().uppercase()) }
                .getOrElse { throw UnknownUtilityTypeException(value) }
    }
}
