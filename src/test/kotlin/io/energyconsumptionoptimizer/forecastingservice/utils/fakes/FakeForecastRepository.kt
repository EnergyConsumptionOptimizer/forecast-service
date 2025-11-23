package io.energyconsumptionoptimizer.forecastingservice.utils.fakes

import io.energyconsumptionoptimizer.forecastingservice.domain.ForecastedConsumption
import io.energyconsumptionoptimizer.forecastingservice.domain.port.ForecastRepository
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastId
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType
import java.util.concurrent.ConcurrentHashMap

class FakeForecastRepository : ForecastRepository {
    private val forecasts = ConcurrentHashMap<ForecastId, ForecastedConsumption>()

    override suspend fun save(forecast: ForecastedConsumption): ForecastedConsumption {
        forecasts.values.firstOrNull { it.utilityType == forecast.utilityType }?.let { forecasts.remove(it.id) }
        forecasts[forecast.id] = forecast
        return forecast
    }

    override suspend fun findAll(): List<ForecastedConsumption> = forecasts.values.toList()

    override suspend fun findById(id: ForecastId): ForecastedConsumption? = forecasts[id]

    override suspend fun findByUtility(utilityType: UtilityType): ForecastedConsumption? =
        forecasts.values.firstOrNull { it.utilityType == utilityType }

    override suspend fun remove(forecast: ForecastedConsumption): Boolean = forecasts.remove(forecast.id) != null

    override suspend fun removeById(id: ForecastId): Boolean = forecasts.remove(id) != null

    override suspend fun removeByUtility(utilityType: UtilityType): Boolean {
        val forecast = forecasts.values.firstOrNull { it.utilityType == utilityType }
        return if (forecast != null) {
            forecasts.remove(forecast.id) != null
        } else {
            false
        }
    }

    fun clear() {
        forecasts.clear()
    }
}
