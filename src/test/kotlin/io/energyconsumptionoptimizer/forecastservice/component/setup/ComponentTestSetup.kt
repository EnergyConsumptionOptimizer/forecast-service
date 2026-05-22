package io.energyconsumptionoptimizer.forecastservice.component.setup

import arrow.core.raise.Raise
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.application.ports.dto.DataPoint
import io.energyconsumptionoptimizer.forecastservice.application.ports.dto.HistoricalDataProvided
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.HistoricalDataProvider
import io.energyconsumptionoptimizer.forecastservice.application.services.ComputeForecastServiceImpl
import io.energyconsumptionoptimizer.forecastservice.application.services.GetForecastsServiceImpl
import io.energyconsumptionoptimizer.forecastservice.domain.value.HistoricalDataPoint
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import io.energyconsumptionoptimizer.forecastservice.infrastructure.algorithm.RandomForestForecast
import io.energyconsumptionoptimizer.forecastservice.infrastructure.events.MongoOutboxEventPublisher
import io.energyconsumptionoptimizer.forecastservice.infrastructure.metrics.OtelBusinessMetrics
import io.energyconsumptionoptimizer.forecastservice.infrastructure.persistence.mongo.MongoForecastRepository
import io.energyconsumptionoptimizer.forecastservice.infrastructure.persistence.mongo.MongoUnitOfWork
import io.energyconsumptionoptimizer.forecastservice.integration.persistence.MongoSetup
import io.energyconsumptionoptimizer.forecastservice.presentation.rest.controller.ForecastController
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

object ComponentTestSetup {
    private const val DB_NAME = "test-forecast-component"
    private var client: CoroutineClient? = null

    val available get() = MongoSetup.available

    fun startMongo() {
        MongoSetup.start()
        if (available) client = KMongo.createClient(MongoSetup.CONNECTION_STRING).coroutine
    }

    fun stopMongo() {
        client?.close()
        client = null
        MongoSetup.stop()
    }

    fun clearDatabase() = client?.let { runBlocking { MongoSetup.clearDatabase(it, DB_NAME) } }

    fun repository() = MongoForecastRepository(client!!, DB_NAME)

    val forecastController by lazy {
        ForecastController(GetForecastsServiceImpl(repository()))
    }

    fun createComputeService(provider: HistoricalDataProvider) =
        ComputeForecastServiceImpl(
            repository = repository(),
            historicalDataProvider = provider,
            forecastAlgorithm = RandomForestForecast(),
            uow = MongoUnitOfWork(client!!),
            eventPublisher = MongoOutboxEventPublisher(client!!, DB_NAME),
            metrics = OtelBusinessMetrics(),
        )

    fun stubHistoricalDataProvider(data: List<HistoricalDataPoint>) =
        object : HistoricalDataProvider {
            context(raise: Raise<ApplicationError.DataProviderError>)
            override suspend fun fetchByUtility(utilityType: UtilityType) =
                HistoricalDataProvided(data.map { DataPoint(it.date, it.value.amount) })
        }
}
