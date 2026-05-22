package io.energyconsumptionoptimizer.forecastservice.application.services

import arrow.core.raise.Raise
import arrow.core.raise.context.raise
import arrow.core.raise.context.withError
import arrow.core.raise.either
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.application.ports.inbound.ComputeForecastService
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.BusinessMetricsPort
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.EventPublisher
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.ForecastAlgorithm
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.HistoricalDataProvider
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.UnitOfWork
import io.energyconsumptionoptimizer.forecastservice.domain.DomainError
import io.energyconsumptionoptimizer.forecastservice.domain.entity.Forecast
import io.energyconsumptionoptimizer.forecastservice.domain.ports.ForecastRepository
import io.energyconsumptionoptimizer.forecastservice.domain.service.DefaultForecastPolicy
import io.energyconsumptionoptimizer.forecastservice.domain.value.ConsumptionValue
import io.energyconsumptionoptimizer.forecastservice.domain.value.ForecastedDataPoint
import io.energyconsumptionoptimizer.forecastservice.domain.value.ForecastedDataSeries
import io.energyconsumptionoptimizer.forecastservice.domain.value.HistoricalDataPoint
import io.energyconsumptionoptimizer.forecastservice.domain.value.HistoricalDataSeries
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType

class ComputeForecastServiceImpl(
    private val repository: ForecastRepository,
    private val historicalDataProvider: HistoricalDataProvider,
    private val forecastAlgorithm: ForecastAlgorithm,
    private val uow: UnitOfWork,
    private val eventPublisher: EventPublisher,
    private val metrics: BusinessMetricsPort,
) : ComputeForecastService {
    context(raise: Raise<ApplicationError>)
    override suspend fun computeAll() {
        val results =
            UtilityType.entries.map { utilityType ->
                val outcome = either { compute(utilityType) }
                utilityType to outcome
            }

        val successes = mutableListOf<UtilityType>()
        val errors = mutableMapOf<UtilityType, ApplicationError>()
        results.forEach { (utilityType, outcome) ->
            outcome.fold(
                ifLeft = { errors[utilityType] = it },
                ifRight = { successes.add(utilityType) },
            )
        }

        if (errors.isNotEmpty()) {
            raise(ApplicationError.BatchComputationFailed(successes, errors))
        }
    }

    context(raise: Raise<ApplicationError>)
    override suspend fun compute(utilityType: UtilityType) {
        val historicalDataSeries = getHistoricalConsumptionData(utilityType)
        val forecastHorizon = DefaultForecastPolicy.maxHorizon(utilityType)
        val dataSeries = generateForecastedDataSeries(historicalDataSeries, forecastHorizon, utilityType)

        val existing = repository.findByUtility(utilityType)
        val forecast = existing?.apply { recompute(dataSeries) } ?: Forecast.create(utilityType, dataSeries)

        // Outbox pattern
        persistAndPublish(forecast)

        metrics.recordForecastComputation()
    }

    context(raise: Raise<ApplicationError>)
    private suspend fun persistAndPublish(forecast: Forecast) {
        uow.executeTransactionally {
            repository.save(forecast)
            forecast.pullDomainEvents().forEach { event ->
                eventPublisher.publish(event)
            }
        }
    }

    context(raise: Raise<ApplicationError>)
    private suspend fun getHistoricalConsumptionData(utilityType: UtilityType): HistoricalDataSeries {
        val historicalDataProvided = historicalDataProvider.fetchByUtility(utilityType)
        val historicalDataSeries =
            withError({ e: DomainError -> ApplicationError.Domain(e) }) {
                val points =
                    historicalDataProvided.predictions.map { pointDto ->
                        val consumption = ConsumptionValue.of(pointDto.value)
                        HistoricalDataPoint.of(pointDto.date, consumption)
                    }
                val series = HistoricalDataSeries.of(points)
                series
            }
        return historicalDataSeries
    }

    context(raise: Raise<ApplicationError>)
    private suspend fun generateForecastedDataSeries(
        historicalDataSeries: HistoricalDataSeries,
        forecastHorizon: Int,
        utilityType: UtilityType,
    ): ForecastedDataSeries {
        val prediction = forecastAlgorithm.forecast(historicalDataSeries, forecastHorizon)

        val dataSeries =
            withError({ e: DomainError -> ApplicationError.Domain(e) }) {
                val points =
                    prediction.predictions.map { pointDto ->
                        val consumption = ConsumptionValue.of(pointDto.value)
                        ForecastedDataPoint.of(pointDto.date, consumption)
                    }
                val series = ForecastedDataSeries.of(points)
                DefaultForecastPolicy.validate(utilityType, series)
                series
            }
        return dataSeries
    }
}
