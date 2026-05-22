package io.energyconsumptionoptimizer.forecastservice.presentation.rest.middleware

import arrow.core.raise.Raise
import arrow.core.raise.recover
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.infrastructure.metrics.ErrorMetrics
import io.energyconsumptionoptimizer.forecastservice.presentation.ApiErrorResponse
import io.energyconsumptionoptimizer.forecastservice.presentation.WebApiErrorCode
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ParameterConversionException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<ParameterConversionException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiErrorResponse(WebApiErrorCode.VALIDATION_ERROR, cause.message ?: "Invalid parameter"),
            )
        }

        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiErrorResponse(WebApiErrorCode.VALIDATION_ERROR, cause.message ?: "Invalid parameter"),
            )
        }

        exception<Exception> { call, cause ->
            call.application.environment.log
                .error("Unhandled exception", cause)
            ErrorMetrics.errorsTotal.add(1)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"),
            )
        }
    }
}

suspend inline fun ApplicationCall.respondRaised(crossinline block: suspend Raise<ApplicationError>.() -> Any) {
    recover({ respond(HttpStatusCode.OK, block(this)) }) { error ->
        val (status, code, message) =
            when (error) {
                is ApplicationError.ForecastNotFound -> {
                    Triple(
                        HttpStatusCode.NotFound,
                        "NOT_FOUND",
                        "Forecast not found for utility: ${error.utilityType}",
                    )
                }

                is ApplicationError.AlgorithmError,
                is ApplicationError.DataProviderError,
                -> {
                    Triple(
                        HttpStatusCode.BadGateway,
                        "EXTERNAL_SERVICE_ERROR",
                        "Failed to retrieve or process forecast data",
                    )
                }

                else -> {
                    Triple(
                        HttpStatusCode.InternalServerError,
                        "INTERNAL_ERROR",
                        "An unexpected business error occurred",
                    )
                }
            }
        application.environment.log.warn("Error [$code]: $message")
        respond(status, ApiErrorResponse(code, message))
    }
}
