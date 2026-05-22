package io.energyconsumptionoptimizer.forecastservice.application.ports.outbound

import arrow.core.raise.Raise
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError

interface UnitOfWork {
    context(raise: Raise<ApplicationError.TransactionFailed>)
    suspend fun <T> executeTransactionally(operation: suspend () -> T): T
}
