package io.energyconsumptionoptimizer.forecastservice.infrastructure.persistence.mongo

import com.mongodb.reactivestreams.client.ClientSession
import kotlinx.coroutines.currentCoroutineContext

suspend fun currentMongoSession(): ClientSession? = currentCoroutineContext()[MongoSession]?.session

class MongoSession(
    val session: ClientSession,
) : kotlin.coroutines.AbstractCoroutineContextElement(MongoSession) {
    companion object Key : kotlin.coroutines.CoroutineContext.Key<MongoSession>
}
