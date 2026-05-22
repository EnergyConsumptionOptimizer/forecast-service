package io.energyconsumptionoptimizer.forecastservice.domain.ports

import io.energyconsumptionoptimizer.forecastservice.domain.entity.Forecast
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType

interface ForecastRepository {
    suspend fun save(forecast: Forecast)

    suspend fun findAll(): List<Forecast>

    suspend fun findByUtility(utilityType: UtilityType): Forecast?

    suspend fun remove(forecast: Forecast)
}
