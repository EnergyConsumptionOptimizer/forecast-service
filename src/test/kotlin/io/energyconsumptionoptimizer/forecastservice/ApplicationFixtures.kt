package io.energyconsumptionoptimizer.forecastservice

import arrow.core.raise.Raise
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.forecastPoint
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.historicalData
import io.energyconsumptionoptimizer.forecastservice.DomainFixtures.tomorrow
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.application.ports.dto.AlgorithmPrediction
import io.energyconsumptionoptimizer.forecastservice.application.ports.dto.DataPoint
import io.energyconsumptionoptimizer.forecastservice.application.ports.dto.HistoricalDataProvided
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.ForecastAlgorithm
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.HistoricalDataProvider
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.UnitOfWork
import io.energyconsumptionoptimizer.forecastservice.domain.value.ForecastedDataSeries
import io.energyconsumptionoptimizer.forecastservice.domain.value.HistoricalDataSeries
import io.mockk.CapturingSlot
import io.mockk.coEvery
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

object ApplicationFixtures {
    val validHistoricalSeries = historicalData(30)
    val validForecastPoints =
        listOf(
            forecastPoint(tomorrow, 50.0),
            forecastPoint(tomorrow.plus(1, DateTimeUnit.DAY), 60.0),
        )

    val validHistoricalDataDto =
        HistoricalDataProvided(
            validHistoricalSeries.points.map { DataPoint(it.date, it.value.amount) },
        )

    val validAlgorithmPredictionDto =
        AlgorithmPrediction(
            validForecastPoints.map { DataPoint(it.date, it.value.amount) },
        )

    val emptyHistoricalDataDto = HistoricalDataProvided(emptyList())

    fun dtoFromSeries(series: HistoricalDataSeries): HistoricalDataProvided =
        HistoricalDataProvided(series.points.map { DataPoint(it.date, it.value.amount) })

    fun algorithmPredictionFromSeries(series: ForecastedDataSeries): AlgorithmPrediction =
        AlgorithmPrediction(series.points.map { DataPoint(it.date, it.value.amount) })

    fun setupExecuteTransactionally(
        uow: UnitOfWork,
        slot: CapturingSlot<suspend () -> Unit>,
    ) {
        coEvery {
            with(any<Raise<ApplicationError.TransactionFailed>>()) {
                uow.executeTransactionally(capture(slot))
            }
        } coAnswers {
            slot.captured.invoke()
        }
    }

    fun mockHistoricalDataProviderReturns(
        mock: HistoricalDataProvider,
        data: HistoricalDataProvided,
    ) {
        coEvery {
            with(any<Raise<ApplicationError.DataProviderError>>()) {
                mock.fetchByUtility(any())
            }
        } returns data
    }

    fun mockForecastAlgorithmReturns(
        mock: ForecastAlgorithm,
        prediction: AlgorithmPrediction,
    ) {
        coEvery {
            with(any<Raise<ApplicationError.AlgorithmError>>()) {
                mock.forecast(any(), any())
            }
        } returns prediction
    }
}
