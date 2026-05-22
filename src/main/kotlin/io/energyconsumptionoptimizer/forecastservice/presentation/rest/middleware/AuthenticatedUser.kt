package io.energyconsumptionoptimizer.forecastservice.presentation.rest.middleware

import io.energyconsumptionoptimizer.forecastservice.presentation.ApiErrorResponse
import io.energyconsumptionoptimizer.forecastservice.presentation.WebApiErrorCode
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationFailedCause
import io.ktor.server.response.respond

data class AuthenticatedUser(
    val id: String,
    val username: String,
    val role: String,
)

fun Application.configureAuth() {
    install(Authentication) {
        provider("header-auth") {
            authenticate { context ->
                val request = context.call.request
                val userId = request.headers["x-user-id"]

                if (userId == null) {
                    context.challenge("header-auth", AuthenticationFailedCause.NoCredentials) { challenge, call ->
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiErrorResponse(WebApiErrorCode.AUTH_REQUIRED, "Authentication required"),
                        )
                        challenge.complete()
                    }
                    return@authenticate
                }

                context.principal(
                    AuthenticatedUser(
                        id = userId,
                        username = request.headers["x-user-username"] ?: "",
                        role = request.headers["x-user-role"] ?: "HOUSEHOLD",
                    ),
                )
            }
        }
    }
}
