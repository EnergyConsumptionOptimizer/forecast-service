package io.energyconsumptionoptimizer.forecastservice.infrastructure.persistence.mongo

import kotlinx.serialization.Serializable

@Serializable
data class OutboxEventDocument(
    val eventId: String,
    val aggregateId: String,
    val aggregateType: String,
    val eventType: String,
    val occurredAt: String,
    val payload: String,
    val correlationId: String? = null,
)
