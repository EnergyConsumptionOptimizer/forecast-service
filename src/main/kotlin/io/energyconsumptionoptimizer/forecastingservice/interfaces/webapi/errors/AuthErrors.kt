package io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.errors

class UnauthorizedException(
    message: String? = "Unauthorized",
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class AuthServiceUnavailableException(
    message: String? = "Authentication service unavailable",
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class InvalidTokenException(
    message: String? = "Invalid Token",
    cause: Throwable? = null,
) : RuntimeException(message, cause)

@kotlinx.serialization.Serializable
data class ErrorResponse(
    val error: String,
)
