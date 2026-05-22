package io.energyconsumptionoptimizer.forecastservice.application.ports.inbound

import arrow.core.raise.Raise
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.application.ports.dto.ForecastResponse
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType

interface GetForecastsService {
    context(raise: Raise<ApplicationError>)
    suspend fun getAll(): List<ForecastResponse>

    context(raise: Raise<ApplicationError>)
    suspend fun getByUtility(utilityType: UtilityType): ForecastResponse
}
