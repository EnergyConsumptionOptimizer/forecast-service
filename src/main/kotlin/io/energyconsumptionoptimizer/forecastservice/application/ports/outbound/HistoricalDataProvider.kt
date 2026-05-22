package io.energyconsumptionoptimizer.forecastservice.application.ports.outbound

import arrow.core.raise.Raise
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.application.ports.dto.HistoricalDataProvided
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType

interface HistoricalDataProvider {
    context(raise: Raise<ApplicationError.DataProviderError>)
    suspend fun fetchByUtility(utilityType: UtilityType): HistoricalDataProvided
}
