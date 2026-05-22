package io.energyconsumptionoptimizer.forecastservice.infrastructure.http

import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.context.ensure
import arrow.core.raise.context.raise
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.application.ports.dto.DataPoint
import io.energyconsumptionoptimizer.forecastservice.application.ports.dto.HistoricalDataProvided
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.HistoricalDataProvider
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
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
    context(raise: Raise<ApplicationError.DataProviderError>)
    override suspend fun fetchByUtility(utilityType: UtilityType): HistoricalDataProvided {
        val responseBody =
            catch(
                {
                    val response =
                        httpClient.get(baseUrl) {
                            url {
                                appendPathSegments("api", "internal", "measurements", utilityType.name)
                            }
                        }

                    ensure(response.status.isSuccess()) {
                        ApplicationError.DataProviderError("Monitoring service returned error: ${response.status}")
                    }
                    response.body<MonitoringResponse>()
                },
                { e: Exception ->
                    raise(ApplicationError.DataProviderError("Failed to fetch historical data: ${e.message}"))
                },
            )

        val points =
            responseBody.utilityConsumptions
                .map { point ->
                    val date = point.timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).date
                    DataPoint(date = date, value = point.value)
                }.sortedBy { it.date }

        return HistoricalDataProvided(predictions = points)
    }
}

@Serializable
internal data class MonitoringResponse(
    val utilityConsumptions: List<UtilityConsumptionPointDto>,
)

@Serializable
internal data class UtilityConsumptionPointDto(
    val value: Double,
    val timestamp: Instant,
)
