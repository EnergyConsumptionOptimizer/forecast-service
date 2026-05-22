package io.energyconsumptionoptimizer.forecastservice.domain.event

import kotlin.time.Instant

interface DomainEvent {
    val eventType: String
    val occurredAt: Instant
    val aggregateId: String
    val aggregateType: String
}
