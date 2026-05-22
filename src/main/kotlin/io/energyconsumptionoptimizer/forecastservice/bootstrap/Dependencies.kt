package io.energyconsumptionoptimizer.forecastservice.bootstrap

import io.energyconsumptionoptimizer.forecastservice.OtelSdk
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.BusinessMetricsPort
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.EventPublisher
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.ForecastAlgorithm
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.HistoricalDataProvider
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.UnitOfWork
import io.energyconsumptionoptimizer.forecastservice.application.services.ComputeForecastServiceImpl
import io.energyconsumptionoptimizer.forecastservice.application.services.GetForecastsServiceImpl
import io.energyconsumptionoptimizer.forecastservice.domain.ports.ForecastRepository
import io.energyconsumptionoptimizer.forecastservice.infrastructure.algorithm.RandomForestForecast
import io.energyconsumptionoptimizer.forecastservice.infrastructure.events.MongoOutboxEventPublisher
import io.energyconsumptionoptimizer.forecastservice.infrastructure.http.MonitoringServiceClient
import io.energyconsumptionoptimizer.forecastservice.infrastructure.metrics.OtelBusinessMetrics
import io.energyconsumptionoptimizer.forecastservice.infrastructure.persistence.mongo.MongoForecastRepository
import io.energyconsumptionoptimizer.forecastservice.infrastructure.persistence.mongo.MongoUnitOfWork
import io.energyconsumptionoptimizer.forecastservice.infrastructure.scheduling.ForecastScheduler
import io.energyconsumptionoptimizer.forecastservice.presentation.rest.controller.ForecastController
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

class Dependencies(
    config: Config,
) {
    val config: Config = config

    val mongoClient by lazy {
        KMongo.createClient(config.mongoUri).coroutine
    }

    val httpClient: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }
        }
    }

    val uow: UnitOfWork by lazy {
        MongoUnitOfWork(mongoClient)
    }

    val eventPublisher: EventPublisher by lazy {
        MongoOutboxEventPublisher(mongoClient, config.mongoDatabase)
    }

    val metrics: BusinessMetricsPort by lazy {
        OtelBusinessMetrics()
    }

    val forecastRepository: ForecastRepository by lazy {
        MongoForecastRepository(mongoClient, config.mongoDatabase)
    }

    val forecastAlgorithm: ForecastAlgorithm by lazy {
        RandomForestForecast()
    }

    val historicalDataProvider: HistoricalDataProvider by lazy {
        MonitoringServiceClient(httpClient, config.monitoringServiceUrl)
    }

    val computeForecastService by lazy {
        ComputeForecastServiceImpl(
            repository = forecastRepository,
            historicalDataProvider = historicalDataProvider,
            forecastAlgorithm = forecastAlgorithm,
            uow = uow,
            eventPublisher = eventPublisher,
            metrics = metrics,
        )
    }

    val getForecastsService by lazy {
        GetForecastsServiceImpl(
            repository = forecastRepository,
        )
    }

    val forecastController by lazy {
        ForecastController(getForecastsService)
    }

    val forecastScheduler by lazy {
        ForecastScheduler(
            computeForecastService,
            config.schedulerHour,
            config.schedulerMinute,
        )
    }

    val otelSdk by lazy {
        OtelSdk()
    }

    fun shutdown() {
        otelSdk.shutdown()
        httpClient.close()
    }
}
