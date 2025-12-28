package io.energyconsumptionoptimizer.forecastingservice.interfaces.webapi.resources

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource("/api/health")
class HealthResource
