package io.energyconsumptionoptimizer.forecastservice.interfaces.webapi.routes

import io.energyconsumptionoptimizer.forecastservice.interfaces.webapi.resources.HealthResource
import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable

fun Route.healthRoutes() {
    get<HealthResource> {
        call.respond(HttpStatusCode.OK, HealthResponse())
    }
}

@Serializable
data class HealthResponse(
    val status: String = "OK",
)
