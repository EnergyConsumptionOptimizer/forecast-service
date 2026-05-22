package io.energyconsumptionoptimizer.forecastservice.presentation.rest.routes

import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource("/api/forecasts")
class ForecastsResource {
    @Serializable
    @Resource("{utilityType}")
    class ByUtility(
        @Suppress("unused") val parent: ForecastsResource = ForecastsResource(),
        val utilityType: UtilityType,
    )
}
