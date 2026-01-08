package io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.middleware

import io.energyconsumptionoptimizer.forecastingservice.domain.error.UnknownUtilityTypeException
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.errors.AuthServiceUnavailableException
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.errors.ErrorResponse
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.errors.InvalidTokenException
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.errors.UnauthorizedException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<UnknownUtilityTypeException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Invalid utility type"))
        }

        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Invalid request"))
        }

        exception<InvalidTokenException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse(cause.message ?: "Authentication required"))
        }

        exception<UnauthorizedException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, ErrorResponse(cause.message ?: "Access denied"))
        }

        exception<AuthServiceUnavailableException> { call, cause ->
            call.respond(HttpStatusCode.ServiceUnavailable, ErrorResponse(cause.message ?: "Authentication service unavailable"))
        }

        exception<Exception> { call, _ ->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("An unexpected error occurred"))
        }
    }
}
