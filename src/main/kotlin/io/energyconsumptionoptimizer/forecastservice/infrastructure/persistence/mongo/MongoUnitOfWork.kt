package io.energyconsumptionoptimizer.forecastservice.infrastructure.persistence.mongo

import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.context.raise
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.application.ports.outbound.UnitOfWork
import io.energyconsumptionoptimizer.forecastservice.logger
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withContext
import org.litote.kmongo.coroutine.CoroutineClient

class MongoUnitOfWork(
    private val mongoClient: CoroutineClient,
) : UnitOfWork {
    private val logger = logger()

    context(raise: Raise<ApplicationError.TransactionFailed>)
    override suspend fun <T> executeTransactionally(operation: suspend () -> T): T {
        val session = mongoClient.startSession()
        val startTime = System.currentTimeMillis()
        session.startTransaction()

        val result =
            catch(
                {
                    val result = withContext(MongoSession(session)) { operation() }
                    session.commitTransaction().awaitFirstOrNull()
                    logger.debug("Transaction committed in {}ms", System.currentTimeMillis() - startTime)
                    result
                },
                { e: RuntimeException ->
                    try {
                        session.abortTransaction().awaitFirstOrNull()
                    } catch (_: RuntimeException) {
                    }
                    logger.error("Transaction failed in {}ms", System.currentTimeMillis() - startTime, e)
                    raise(ApplicationError.TransactionFailed(e.message ?: "Transaction failed"))
                },
            )

        try {
            session.close()
        } catch (_: RuntimeException) {
        }

        return result
    }
}
