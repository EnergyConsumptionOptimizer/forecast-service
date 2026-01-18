package io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.middleware

import io.energyconsumptionoptimizer.forecastingservice.domain.error.UnknownUtilityTypeException
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.errors.AuthServiceUnavailableException
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.errors.InvalidTokenException
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.errors.UnauthorizedException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<UnknownUtilityTypeException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiErrorResponse(
                    code = "VALIDATION_ERROR",
                    message = "Invalid utility type",
                    errors = mapOf("utilityType" to (cause.message ?: "Unknown utility type")),
                ),
            )
        }

        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiErrorResponse(code = "BAD_REQUEST", message = cause.message ?: "Invalid request"),
            )
        }

        exception<InvalidTokenException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiErrorResponse(code = "UNAUTHORIZED", message = cause.message ?: "Authentication required"),
            )
        }

        exception<UnauthorizedException> { call, cause ->
            call.respond(
                HttpStatusCode.Forbidden,
                ApiErrorResponse(code = "FORBIDDEN", message = cause.message ?: "Access denied"),
            )
        }

        exception<AuthServiceUnavailableException> { call, cause ->
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                ApiErrorResponse(code = "INFRASTRUCTURE_ERROR", message = cause.message ?: "Authentication service unavailable"),
            )
        }

        exception<Exception> { call, _ ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiErrorResponse(code = "INTERNAL_ERROR", message = "An unexpected error occurred"),
            )
        }
    }
}

@Serializable
data class ApiErrorResponse(
    val code: String,
    val message: String,
    val errors: Map<String, String>? = null,
)
