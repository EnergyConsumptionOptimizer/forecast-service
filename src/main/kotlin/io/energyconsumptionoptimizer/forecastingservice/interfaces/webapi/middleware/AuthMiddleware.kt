package io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.middleware

import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.errors.AuthServiceUnavailableException
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.errors.UnauthorizedException
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import kotlinx.io.IOException

class AuthMiddleware(
    private val httpClient: HttpClient,
) {
    private val userServiceUri = System.getenv("USER_SERVICE_URI") ?: "http://localhost:3000"

    suspend fun verifyUser(token: String) {
        verifyToken(token, "/api/internal/auth/verify")
    }

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
                httpClient.get("$userServiceUri$endpoint") {
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
