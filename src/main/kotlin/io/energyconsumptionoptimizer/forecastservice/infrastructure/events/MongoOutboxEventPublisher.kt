package io.energyconsumptionoptimizer.forecastservice.infrastructure.events

import arrow.core.raise.Raise
import arrow.core.raise.context.ensure
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.EventPublisher
import io.energyconsumptionoptimizer.forecastservice.domain.event.DomainEvent
import io.energyconsumptionoptimizer.forecastservice.domain.event.ForecastComputedEvent
import io.energyconsumptionoptimizer.forecastservice.infrastructure.persistence.mongo.OutboxEventDocument
import io.energyconsumptionoptimizer.forecastservice.infrastructure.persistence.mongo.currentMongoSession
import io.energyconsumptionoptimizer.forecastservice.logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.litote.kmongo.coroutine.CoroutineClient
import java.util.UUID

class MongoOutboxEventPublisher(
    mongoClient: CoroutineClient,
    databaseName: String,
) : EventPublisher {
    private val collection =
        mongoClient
            .getDatabase(databaseName)
            .getCollection<OutboxEventDocument>("outboxevents")

    private val logger = logger()

    context(raise: Raise<ApplicationError.EventPublishFailed>)
    override suspend fun publish(event: DomainEvent) {
        val session = currentMongoSession()
        ensure(session != null) {
            ApplicationError.EventPublishFailed(
                eventId = event.aggregateId,
                cause = "EventPublisher must always be called inside an UnitOfWork",
            )
        }

        val eventId = UUID.randomUUID().toString()
        val payload = serializePayload(event)

        val doc =
            OutboxEventDocument(
                eventId = eventId,
                aggregateId = event.aggregateId,
                aggregateType = event.aggregateType,
                eventType = event.eventType,
                occurredAt = event.occurredAt.toString(),
                payload = payload,
            )

        collection.insertOne(clientSession = session, document = doc)

        logger.debug(
            "Published outbox event type={} aggregateId={} eventId={}",
            event.eventType,
            event.aggregateId,
            eventId,
        )
    }

    private fun serializePayload(event: DomainEvent): String {
        val json =
            buildJsonObject {
                put("eventType", event.eventType)
                put("aggregateId", event.aggregateId)
                put("aggregateType", event.aggregateType)
                put("occurredAt", event.occurredAt.toString())
                when (event) {
                    is ForecastComputedEvent -> {
                        put("utilityType", event.utilityType.name)
                        val dataPointsArray =
                            buildJsonArray {
                                event.dataPoints.points.forEach { point ->
                                    add(
                                        buildJsonObject {
                                            put("date", point.date.toString())
                                            put("value", point.value.amount)
                                        },
                                    )
                                }
                            }
                        put("dataPoints", dataPointsArray)
                    }
                }
            }
        return Json.encodeToString(JsonElement.serializer(), json)
    }
}
