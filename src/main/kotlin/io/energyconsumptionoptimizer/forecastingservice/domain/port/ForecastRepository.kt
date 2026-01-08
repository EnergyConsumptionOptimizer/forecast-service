package io.energyconsumptionoptimizer.forecastingservice.domain.port

import io.energyconsumptionoptimizer.forecastingservice.domain.ForecastedConsumption
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastId
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType

/**
 * Persistence port for storing and retrieving [ForecastedConsumption] entities.
 */
interface ForecastRepository {
    /**
     * Persist a forecast and return the saved instance (including any generated id).
     *
     * @param forecast Forecast to persist.
     * @return The persisted [ForecastedConsumption] (may include generated identifiers).
     */
    suspend fun save(forecast: ForecastedConsumption): ForecastedConsumption

    /**
     * Retrieve all stored forecasts.
     *
     * @return A list of all persisted [ForecastedConsumption] entities.
     */
    suspend fun findAll(): List<ForecastedConsumption>

    /**
     * Find a forecast by its identifier.
     *
     * @param id Identifier of the forecast to find.
     * @return The matching [ForecastedConsumption] or `null` when not found.
     */
    suspend fun findById(id: ForecastId): ForecastedConsumption?

    /**
     * Find the latest forecast for the given utility, if any.
     *
     * @param utilityType Utility type to query for.
     * @return The most recent [ForecastedConsumption] for `utilityType` or `null`.
     */
    suspend fun findByUtility(utilityType: UtilityType): ForecastedConsumption?

    /**
     * Remove a forecast entity from persistence.
     *
     * @param forecast Forecast to remove.
     * @return `true` when deletion succeeded, `false` otherwise.
     */
    suspend fun remove(forecast: ForecastedConsumption): Boolean

    /**
     * Remove a forecast by id.
     *
     * @param id Identifier of the forecast to remove.
     * @return `true` when deletion succeeded, `false` otherwise.
     */
    suspend fun removeById(id: ForecastId): Boolean

    /**
     * Remove forecasts for a specific utility.
     *
     * @param utilityType Utility whose forecasts should be removed.
     * @return `true` when deletion succeeded, `false` otherwise.
     */
    suspend fun removeByUtility(utilityType: UtilityType): Boolean
}
