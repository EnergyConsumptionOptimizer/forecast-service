package io.energyconsumptionoptimizer.forecastingservice

import io.energyconsumptionoptimizer.forecastingservice.application.usecases.ComputeForecastUseCase
import io.energyconsumptionoptimizer.forecastingservice.application.usecases.GetForecastsUseCase
import io.energyconsumptionoptimizer.forecastingservice.domain.port.ForecastRepository
import io.energyconsumptionoptimizer.forecastingservice.domain.port.ForecastingAlgorithm
import io.energyconsumptionoptimizer.forecastingservice.domain.port.HistoricalDataProvider
import io.energyconsumptionoptimizer.forecastingservice.domain.port.ThresholdNotifier
import io.energyconsumptionoptimizer.forecastingservice.interfaces.algorithm.RandomForestForecast
import io.energyconsumptionoptimizer.forecastingservice.interfaces.http.MonitoringServiceClient
import io.energyconsumptionoptimizer.forecastingservice.interfaces.http.ThresholdServiceClient
import io.energyconsumptionoptimizer.forecastingservice.interfaces.scheduler.ForecastScheduler
import io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.middleware.AuthMiddleware
import io.energyconsumptionoptimizer.forecastingservice.storage.mongodb.ForecastMongoRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import java.time.LocalTime

/**
 * Runtime configuration values for the application.
 *
 * These values are typically populated from environment variables at startup.
 *
 * @property mongoUri MongoDB connection URI.
 * @property mongoDatabase Database name to use for persistence.
 * @property userServiceUrl Base URL for the user service.
 * @property monitoringServiceUrl Base URL for the monitoring service.
 * @property thresholdServiceUrl Base URL for the threshold evaluation service.
 * @property lookbackDays Number of historical days to include in forecasts.
 * @property schedulerHour Hour of day for the daily scheduler.
 * @property schedulerMinute Minute of hour for the daily scheduler.
 */
data class AppConfig(
    val mongoUri: String,
    val mongoDatabase: String,
    val userServiceUrl: String,
    val monitoringServiceUrl: String,
    val thresholdServiceUrl: String,
    val lookbackDays: Int,
    val schedulerHour: Int,
    val schedulerMinute: Int,
)

/**
 * Composition root holding lazily-initialized application dependencies.
 *
 * Provides domain ports and adapters wired together using the provided
 * [AppConfig]. Properties are lazily created to avoid eager startup costs.
 *
 * @param config Application configuration used to initialize adapters.
 */
class Dependencies(
    private val config: AppConfig,
) {
    val mongoClient: CoroutineClient by lazy {
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
            expectSuccess = true
        }
    }

    val forecastingAlgorithm: ForecastingAlgorithm by lazy {
        RandomForestForecast()
    }

    val historicalDataProvider: HistoricalDataProvider by lazy {
        MonitoringServiceClient(httpClient, config.monitoringServiceUrl)
    }

    val thresholdNotifier: ThresholdNotifier by lazy {
        ThresholdServiceClient(httpClient, config.thresholdServiceUrl)
    }

    val authMiddleware = AuthMiddleware(httpClient, config.userServiceUrl)

    val forecastRepository: ForecastRepository by lazy {
        ForecastMongoRepository(
            mongoClient = mongoClient,
            databaseName = config.mongoDatabase,
        )
    }

    val computeForecastUseCase by lazy {
        ComputeForecastUseCase(
            repository = forecastRepository,
            historicalDataProvider = historicalDataProvider,
            forecastingAlgorithm = forecastingAlgorithm,
            thresholdNotifier = thresholdNotifier,
        )
    }

    val getForecastsUseCase by lazy {
        GetForecastsUseCase(repository = forecastRepository)
    }

    val forecastScheduler by lazy {
        ForecastScheduler(
            computeForecastUseCase,
            LocalTime.of(config.schedulerHour, config.schedulerMinute),
        )
    }

    fun shutdown() {
        httpClient.close()
    }
}
