package io.energyconsumptionoptimizer.forecastservice.application.ports.inbound

import arrow.core.raise.Raise
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType

interface ComputeForecastService {
    context(raise: Raise<ApplicationError>)
    suspend fun compute(utilityType: UtilityType)

    context(raise: Raise<ApplicationError>)
    suspend fun computeAll()
}
