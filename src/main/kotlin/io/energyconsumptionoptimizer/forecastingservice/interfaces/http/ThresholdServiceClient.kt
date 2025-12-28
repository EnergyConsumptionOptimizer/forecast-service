package io.energyconsumptionoptimizer.forecastingservice.interfaces.http

import io.energyconsumptionoptimizer.forecastingservice.domain.port.ThresholdNotifier
import io.energyconsumptionoptimizer.forecastingservice.domain.value.PeriodType
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

class ThresholdServiceClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : ThresholdNotifier {
    override suspend fun notifyForecastAggregations(
        utilityType: UtilityType,
        aggregations: Map<PeriodType, Double>,
    ) {
        httpClient.post("$baseUrl/api/internal/thresholds/evaluations/forecast") {
            contentType(ContentType.Application.Json)
            setBody(createThresholdCheckRequest(utilityType, aggregations))
        }
    }

    private fun createThresholdCheckRequest(
        utilityType: UtilityType,
        aggregations: Map<PeriodType, Double>,
    ) = ForecastThresholdCheck(
        utilityType = utilityType.name,
        aggregations = aggregations.map { (period, value) -> PeriodAggregation(period.name, value) },
    )
}

@Serializable
data class ForecastThresholdCheck(
    val utilityType: String,
    val aggregations: List<PeriodAggregation>,
)

@Serializable
data class PeriodAggregation(
    val periodType: String,
    val value: Double,
)
