package io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.middleware

import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.errors.AuthServiceUnavailableException
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.errors.UnauthorizedException
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import kotlinx.io.IOException

/**
 * Verifies user and admin tokens against an external user service.
 *
 * Performs HTTP calls to validate tokens and translates transport errors
 * into domain-specific exceptions.
 *
 * @param httpClient HTTP client used to contact the user service.
 * @param userServiceUrl Base URL of the user service (for example: `http://host:3000`).
 * @constructor Create middleware with the given HTTP client and target service URL.
 */
class AuthMiddleware(
    private val httpClient: HttpClient,
    private val userServiceUrl: String,
) {
    /**
     * Verify a user token.
     *
     * @param token Token to validate.
     * @throws UnauthorizedException When token is invalid.
     * @throws AuthServiceUnavailableException When the auth service is unreachable.
     */
    suspend fun verifyUser(token: String) {
        verifyToken(token, "/api/internal/auth/verify")
    }

    /**
     * Verify an admin token.
     *
     * @param token Token to validate.
     * @throws UnauthorizedException When token is invalid or lacks admin rights.
     * @throws AuthServiceUnavailableException When the auth service is unreachable.
     */
    suspend fun verifyAdmin(token: String) {
        verifyToken(token, "/api/internal/auth/verify-admin")
    }

    @Suppress("RedundantSuspendModifier")
    private suspend fun verifyToken(
        token: String,
        endpoint: String,
    ) {
        try {
            val response =
                httpClient.get("$userServiceUrl$endpoint") {
                    header("Cookie", "authToken=$token")
                }

            if (response.status != HttpStatusCode.OK) {
                throw UnauthorizedException("Token verification failed")
            }
        } catch (e: ResponseException) {
            throw UnauthorizedException(e.message, e)
        } catch (e: IOException) {
            throw AuthServiceUnavailableException("Auth service unreachable", e)
        }
    }
}
