package io.energyconsumptionoptimizer.forecastingservice.domain.port

import io.energyconsumptionoptimizer.forecastingservice.domain.ForecastedConsumption
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastId
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType

interface ForecastRepository {
    suspend fun save(forecast: ForecastedConsumption): ForecastedConsumption

    suspend fun findAll(): List<ForecastedConsumption>

    suspend fun findById(id: ForecastId): ForecastedConsumption?

    suspend fun findByUtility(utilityType: UtilityType): ForecastedConsumption?

    suspend fun remove(forecast: ForecastedConsumption): Boolean

    suspend fun removeById(id: ForecastId): Boolean

    suspend fun removeByUtility(utilityType: UtilityType): Boolean
}
