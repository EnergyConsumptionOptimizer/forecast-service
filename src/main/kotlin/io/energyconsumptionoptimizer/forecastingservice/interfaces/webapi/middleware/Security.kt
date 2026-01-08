package io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.middleware

import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.errors.InvalidTokenException
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationProvider

/**
 * Configure application authentication using cookie-based providers.
 *
 * Registers two providers: `auth-user` and `auth-admin` which delegate token
 * verification to the supplied [AuthMiddleware]. When verification succeeds the
 * `UserPrincipal` is attached to the authentication context.
 *
 * @param authMiddleware Middleware used to validate tokens.
 */
fun Application.configureAuthentication(authMiddleware: AuthMiddleware) {
    install(Authentication) {
        fun registerCookieAuth(
            name: String,
            verifyLogic: suspend (String) -> Unit,
        ) {
            class ProviderConfig(
                name: String,
            ) : AuthenticationProvider.Config(name)
            val provider =
                object : AuthenticationProvider(ProviderConfig(name)) {
                    override suspend fun onAuthenticate(context: AuthenticationContext) {
                        val token =
                            context.call.request.cookies["authToken"]
                                ?: throw InvalidTokenException()

                        verifyLogic(token)
                        context.principal(UserPrincipal(token))
                    }
                }

            register(provider)
        }

        registerCookieAuth("auth-user") { token ->
            authMiddleware.verifyUser(token)
        }

        registerCookieAuth("auth-admin") { token ->
            authMiddleware.verifyAdmin(token)
        }
    }
}
