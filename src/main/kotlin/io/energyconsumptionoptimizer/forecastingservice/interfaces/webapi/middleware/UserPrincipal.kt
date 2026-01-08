package io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.middleware

/**
 * Represents an authenticated principal attached to the request context.
 *
 * @property token Raw authentication token associated with the principal.
 */
data class UserPrincipal(
    val token: String,
)
