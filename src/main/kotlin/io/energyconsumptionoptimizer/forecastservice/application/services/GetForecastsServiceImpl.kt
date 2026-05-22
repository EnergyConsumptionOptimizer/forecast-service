package io.energyconsumptionoptimizer.forecastservice.application.services

import arrow.core.raise.Raise
import arrow.core.raise.context.raise
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.application.ports.dto.DataPoint
import io.energyconsumptionoptimizer.forecastservice.application.ports.dto.ForecastResponse
import io.energyconsumptionoptimizer.forecastservice.application.ports.inbound.GetForecastsService
import io.energyconsumptionoptimizer.forecastservice.domain.entity.Forecast
import io.energyconsumptionoptimizer.forecastservice.domain.ports.ForecastRepository
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType

class GetForecastsServiceImpl(
    private val repository: ForecastRepository,
) : GetForecastsService {
    context(raise: Raise<ApplicationError>)
    override suspend fun getAll(): List<ForecastResponse> = repository.findAll().map { it.toDTO() }

    context(raise: Raise<ApplicationError>)
    override suspend fun getByUtility(utilityType: UtilityType): ForecastResponse {
        val forecast =
            repository.findByUtility(utilityType)
                ?: raise(ApplicationError.ForecastNotFound(utilityType))

        return forecast.toDTO()
    }

    private fun Forecast.toDTO(): ForecastResponse =
        ForecastResponse(
            utilityType = this.utilityType,
            dataPoints =
                this.series.points.map { point ->
                    DataPoint(
                        date = point.date,
                        value = point.value.amount,
                    )
                },
            computedAt = this.computedAt,
        )
}
