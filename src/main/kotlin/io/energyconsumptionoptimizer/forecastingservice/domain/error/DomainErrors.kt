package io.energyconsumptionoptimizer.forecastingservice.domain.error

abstract class DomainException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class UnknownUtilityTypeException(
    value: String,
) : DomainException(
        message = "Unknown utility type: '$value'",
    )
