package io.energyconsumptionoptimizer.forecastservice.application.ports.dto

import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import kotlin.time.Instant

data class ForecastResponse(
    val utilityType: UtilityType,
    val dataPoints: List<DataPoint>,
    val computedAt: Instant,
)
