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

/**
 * HTTP client adapter that implements [ThresholdNotifier] by forwarding
 * aggregation summaries to an external threshold evaluation service.
 *
 * @param httpClient Ktor [HttpClient] used to perform requests.
 * @param baseUrl Base URL of the threshold evaluation service.
 */
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

/**
 * Request payload sent to the threshold evaluation service.
 *
 * @property utilityType Name of the `UtilityType` being evaluated.
 * @property aggregations Aggregated values converted to [PeriodAggregation] entries.
 */
@Serializable
data class ForecastThresholdCheck(
    val utilityType: String,
    val aggregations: List<PeriodAggregation>,
)

/**
 * Single aggregation entry used in [ForecastThresholdCheck].
 *
 * @property periodType Name of the aggregation period (e.g. `ONE_DAY`).
 * @property value Aggregated numeric value for the period.
 */
@Serializable
data class PeriodAggregation(
    val periodType: String,
    val value: Double,
)
