package io.energyconsumptionoptimizer.forecastservice.interfaces.webapi.resources

import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource("/api/forecasts")
data class ForecastResource(
    val utilityType: UtilityType? = null,
) {
    @Serializable
    @Resource("{utilityType}")
    data class ByType(
        val parent: ForecastResource = ForecastResource(),
        val utilityType: UtilityType,
    )
}
