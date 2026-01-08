package io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.resources

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource("/health")
class HealthResource
