package io.energyconsumptionoptimizer.forecastingservice.domain.error

/**
 * Base exception for domain-level errors.
 */
abstract class DomainException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Thrown when a provided utility type string cannot be parsed into a supported `UtilityType`.
 */
class UnknownUtilityTypeException(
    value: String,
) : DomainException(
        message = "Unknown utility type: '$value'",
    )
