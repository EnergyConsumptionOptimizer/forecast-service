package io.energyconsumptionoptimizer.forecastservice.presentation

import kotlinx.serialization.Serializable

object WebApiErrorCode {
    const val AUTH_REQUIRED = "AUTH_REQUIRED"
    const val VALIDATION_ERROR = "VALIDATION_ERROR"
}

@Serializable
data class ApiErrorResponse(
    val code: String,
    val message: String,
)
