package io.energyconsumptionoptimizer.forecastingservice.application.usecases

import io.energyconsumptionoptimizer.forecastingservice.domain.ForecastedConsumption
import io.energyconsumptionoptimizer.forecastingservice.domain.port.ForecastRepository
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType

class GetForecastsUseCase(
    private val repository: ForecastRepository,
) {
    suspend fun getAll(): List<ForecastedConsumption> = repository.findAll()

    suspend fun getByUtility(utilityType: UtilityType): ForecastedConsumption? = repository.findByUtility(utilityType)
}
