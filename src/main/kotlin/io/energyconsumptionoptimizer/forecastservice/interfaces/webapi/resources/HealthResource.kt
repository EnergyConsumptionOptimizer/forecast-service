package io.energyconsumptionoptimizer.forecastservice.interfaces.webapi.resources

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource("/health")
class HealthResource
