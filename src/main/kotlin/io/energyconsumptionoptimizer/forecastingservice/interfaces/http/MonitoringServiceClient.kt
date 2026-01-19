package io.energyconsumptionoptimizer.forecastingservice.interfaces.http

import io.energyconsumptionoptimizer.forecastingservice.domain.port.HistoricalData
import io.energyconsumptionoptimizer.forecastingservice.domain.port.HistoricalDataProvider
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ConsumptionValue
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Instant

class MonitoringServiceClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : HistoricalDataProvider {
    override suspend fun fetchAggregatedHistoricalData(utilityType: UtilityType): List<HistoricalData> {
        val response =
            httpClient.get(baseUrl) {
                url {
                    appendPathSegments("api", "internal", "measurements", utilityType.name)
                }
            }

        require(response.status.isSuccess()) {
            "Monitoring service returned error: ${response.status}"
        }

        return response
            .body<MonitoringResponse>()
            .utilityConsumptions
            .map { point ->
                HistoricalData(
                    timestamp = point.timestamp.toLocalDateTime(TimeZone.UTC).date,
                    value = ConsumptionValue.of(point.value),
                )
            }.sortedBy { it.timestamp }
    }
}

@Serializable
private data class MonitoringResponse(
    val utilityConsumptions: List<UtilityConsumptionPointDto>,
)

@Serializable
private data class UtilityConsumptionPointDto(
    val value: Double,
    val timestamp: Instant,
)
