package io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.routes

import io.energyconsumptionoptimizer.forecastingservice.application.usecases.GetForecastsUseCase
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.resources.ForecastResource
import io.energyconsumptionoptimizer.forecastingservice.presentation.dto.toListDTO
import io.ktor.server.auth.authenticate
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route

fun Route.forecastRoutes(getForecastsUseCase: GetForecastsUseCase) {
    authenticate("auth-user") {
        get<ForecastResource> { resource ->
            val forecasts =
                resource.utilityType?.let { type ->
                    listOfNotNull(getForecastsUseCase.getByUtility(type))
                } ?: getForecastsUseCase.getAll()

            call.respond(forecasts.toListDTO())
        }

        get<ForecastResource.ByType> { resource ->
            val forecasts = listOfNotNull(getForecastsUseCase.getByUtility(resource.utilityType))
            call.respond(forecasts.toListDTO())
        }
    }
}
