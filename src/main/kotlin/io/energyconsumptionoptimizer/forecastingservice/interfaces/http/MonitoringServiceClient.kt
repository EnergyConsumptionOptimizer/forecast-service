package io.energyconsumptionoptimizer.forecastingservice.interfaces.http

import io.energyconsumptionoptimizer.forecastingservice.domain.port.HistoricalData
import io.energyconsumptionoptimizer.forecastingservice.domain.port.HistoricalDataProvider
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ConsumptionValue
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * HTTP adapter that fetches aggregated historical observations from the
 * monitoring service and converts them into domain [HistoricalData].
 *
 * The adapter expects the monitoring service to return a list of measurements
 * where each measurement corresponds to a single day. Responses are validated
 * and converted to [ConsumptionValue] instances.
 *
 * @param httpClient Ktor [HttpClient] used to perform requests.
 * @param baseUrl Base URL of the monitoring service.
 */
class MonitoringServiceClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : HistoricalDataProvider {
    override suspend fun fetchAggregatedHistoricalData(utilityType: UtilityType): List<HistoricalData> {
        val response =
            httpClient.get("$baseUrl/measurements") {
                parameter("utilityType", utilityType.name)
            }

        require(response.status.isSuccess()) {
            "Monitoring service returned error: ${response.status}"
        }

        return response
            .body<MonitoringHistoricalResponse>()
            .measurements
            .groupBy {
                Instant
                    .parse(it.timestamp)
                    .toLocalDateTime(TimeZone.UTC)
                    .date
            }.map { (date, measurementsForDay) ->
                val measurement =
                    measurementsForDay.singleOrNull()
                        ?: error("Expected exactly one measurement for $date, but found ${measurementsForDay.size}")

                HistoricalData(
                    timestamp = date,
                    value = ConsumptionValue.of(measurement.consumptionValue),
                )
            }.sortedBy { it.timestamp }
    }
}

/**
 * Exception thrown when monitoring service responses cannot be processed.
 *
 * @param message Diagnostic message describing the failure.
 */
class MonitoringServiceException(
    message: String,
) : RuntimeException(message)

@Serializable
data class MonitoringHistoricalResponse(
    val measurements: List<HistoricalMeasurementDto>,
)

@Serializable
data class HistoricalMeasurementDto(
    val utilityType: String,
    val consumptionValue: Double,
    val unit: String,
    val timestamp: String,
)
