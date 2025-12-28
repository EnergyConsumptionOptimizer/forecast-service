package io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.middleware

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationFailedCause
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable

fun Application.configureAuthentication(client: HttpClient) {
    val baseUri =
        environment.config.propertyOrNull("user.service.uri")?.getString()
            ?: "http://user-service:3000"

    install(Authentication) {
        upstream("user-auth", "$baseUri/api/internal/auth/verify", client)
        upstream("admin-auth", "$baseUri/api/internal/auth/verify-admin", client)
    }
}

data class UserPrincipal(
    val id: String,
    val rawToken: String,
)

fun Route.authenticated(block: Route.() -> Unit) = authenticate("user-auth", build = block)

// fun Route.authenticatedAdmin(block: Route.() -> Unit) = authenticate("admin-auth", build = block)

private fun AuthenticationConfig.upstream(
    name: String,
    url: String,
    client: HttpClient,
) {
    register(
        object : AuthenticationProvider(SimpleConfig(name)) {
            override suspend fun onAuthenticate(context: AuthenticationContext) {
                val token = context.call.request.cookies["authToken"]

                if (token.isNullOrBlank()) {
                    context.respondAuthFailure(AuthenticationFailedCause.NoCredentials, HttpStatusCode.Unauthorized)
                    return
                }

                val principal = client.verifyToken(url, token)

                if (principal != null) {
                    context.principal(principal)
                } else {
                    context.respondAuthFailure(AuthenticationFailedCause.InvalidCredentials, HttpStatusCode.Forbidden)
                }
            }
        },
    )
}

@Suppress("RedundantSuspendModifier")
private suspend fun HttpClient.verifyToken(
    url: String,
    token: String,
): UserPrincipal? =
    runCatching {
        get(url) { header(HttpHeaders.Cookie, "authToken=$token") }
            .takeIf { it.status.isSuccess() }
            ?.body<AuthResponse>()
            ?.let { UserPrincipal(it.id, token) }
    }.getOrNull()

private fun AuthenticationContext.respondAuthFailure(
    cause: AuthenticationFailedCause,
    status: HttpStatusCode,
) {
    challenge("AuthFailure", cause) { challenge, call ->
        call.respond(status)
        challenge.complete()
    }
}

private class SimpleConfig(
    name: String?,
) : AuthenticationProvider.Config(name)

@Serializable private data class AuthResponse(
    val id: String,
)
