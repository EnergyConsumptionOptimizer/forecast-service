package io.energyconsumptionoptimizer.forecastservice.domain.value

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import io.energyconsumptionoptimizer.forecastservice.domain.DomainError

@JvmInline
value class ConsumptionValue private constructor(
    val amount: Double,
) : Comparable<ConsumptionValue> {
    override fun compareTo(other: ConsumptionValue): Int = amount.compareTo(other.amount)

    companion object {
        context(raise: Raise<DomainError.InvalidConsumptionValue>)
        fun of(amount: Double): ConsumptionValue {
            raise.ensure(amount >= 0.0) { DomainError.InvalidConsumptionValue(amount) }
            return ConsumptionValue(amount)
        }
    }
}
