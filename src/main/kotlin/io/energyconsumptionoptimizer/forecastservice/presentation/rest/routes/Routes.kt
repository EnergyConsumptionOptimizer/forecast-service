package io.energyconsumptionoptimizer.forecastservice.presentation.rest.routes

import io.energyconsumptionoptimizer.forecastservice.presentation.rest.controller.ForecastController
import io.energyconsumptionoptimizer.forecastservice.presentation.rest.middleware.respondRaised
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable

fun Route.forecastRoutes(controller: ForecastController) {
    authenticate("header-auth") {
        get<ForecastsResource> {
            call.respondRaised { controller.getAll() }
        }

        get<ForecastsResource.ByUtility> { request ->
            call.respondRaised { controller.getByUtility(request.utilityType) }
        }
    }
}

fun Route.healthRoutes() {
    get("/health") {
        call.respond(HttpStatusCode.OK, HealthResponse())
    }
}

@Serializable
data class HealthResponse(
    val status: String = "OK",
)
