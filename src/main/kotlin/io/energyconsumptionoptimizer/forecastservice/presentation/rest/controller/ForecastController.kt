package io.energyconsumptionoptimizer.forecastservice.presentation.rest.controller

import arrow.core.raise.Raise
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.application.ports.inbound.GetForecastsService
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import io.energyconsumptionoptimizer.forecastservice.presentation.dto.ForecastListResponse
import io.energyconsumptionoptimizer.forecastservice.presentation.dto.ForecastResponse
import io.energyconsumptionoptimizer.forecastservice.presentation.dto.toPresentationDTO

class ForecastController(
    private val getForecastsService: GetForecastsService,
) {
    context(raise: Raise<ApplicationError>)
    suspend fun getAll(): ForecastListResponse {
        val forecasts = getForecastsService.getAll()
        return ForecastListResponse(forecasts.map { it.toPresentationDTO() })
    }

    context(raise: Raise<ApplicationError>)
    suspend fun getByUtility(utilityType: UtilityType): ForecastResponse {
        val forecast = getForecastsService.getByUtility(utilityType)
        return forecast.toPresentationDTO()
    }
}
