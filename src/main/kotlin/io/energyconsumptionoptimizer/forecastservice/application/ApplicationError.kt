package io.energyconsumptionoptimizer.forecastservice.application

import io.energyconsumptionoptimizer.forecastservice.domain.DomainError
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType

sealed interface ApplicationError {
    data class BatchComputationFailed(
        val successes: List<UtilityType>,
        val failures: Map<UtilityType, ApplicationError>,
    ) : ApplicationError

    data class DataProviderError(
        val message: String,
    ) : ApplicationError

    @JvmInline
    value class Domain(
        val error: DomainError,
    ) : ApplicationError

    data class AlgorithmError(
        val message: String,
    ) : ApplicationError

    data class ForecastNotFound(
        val utilityType: UtilityType,
    ) : ApplicationError

    data class TransactionFailed(
        val cause: String,
    ) : ApplicationError

    data class EventPublishFailed(
        val eventId: String,
        val cause: String,
    ) : ApplicationError
}
