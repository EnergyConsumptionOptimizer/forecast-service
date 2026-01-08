package io.energyconsumptionoptimizer.forecastingservice.application.usecases

import io.energyconsumptionoptimizer.forecastingservice.domain.ForecastedConsumption
import io.energyconsumptionoptimizer.forecastingservice.domain.port.ForecastRepository
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType

/**
 * Provides retrieval operations for stored forecasts.
 *
 * Serves as an application-layer use case that delegates persistence reads to
 * the [ForecastRepository]. Consumers can obtain all forecasts or query for a
 * specific utility.
 *
 * @param repository Repository used to read persisted [ForecastedConsumption] entities.
 * @constructor Creates a retrieval use case with the supplied repository.
 */
class GetForecastsUseCase(
    private val repository: ForecastRepository,
) {
    /**
     * Returns all persisted forecasts.
     *
     * This method forwards to [ForecastRepository.findAll] and returns the
     * resulting list.
     *
     * @return A `List` of [ForecastedConsumption] currently stored in the repository.
     */
    suspend fun getAll(): List<ForecastedConsumption> = repository.findAll()

    /**
     * Returns the forecast for the specified [utilityType], if present.
     *
     * @param utilityType Utility for which to retrieve the forecast, see [UtilityType].
     * @return The matching [ForecastedConsumption], or `null` if none is found.
     */
    suspend fun getByUtility(utilityType: UtilityType): ForecastedConsumption? = repository.findByUtility(utilityType)
}
