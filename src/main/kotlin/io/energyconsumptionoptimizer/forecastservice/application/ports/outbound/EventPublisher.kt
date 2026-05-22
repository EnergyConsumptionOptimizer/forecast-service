package io.energyconsumptionoptimizer.forecastservice.application.ports.outbound

import arrow.core.raise.Raise
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.domain.event.DomainEvent

interface EventPublisher {
    context(raise: Raise<ApplicationError.EventPublishFailed>)
    suspend fun publish(event: DomainEvent)
}
