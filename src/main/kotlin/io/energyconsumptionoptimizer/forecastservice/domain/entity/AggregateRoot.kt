package io.energyconsumptionoptimizer.forecastservice.domain.entity

import io.energyconsumptionoptimizer.forecastservice.domain.event.DomainEvent

abstract class AggregateRoot {
    private val domainEvents = mutableListOf<DomainEvent>()

    protected fun addDomainEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    fun pullDomainEvents(): List<DomainEvent> {
        val events = domainEvents.toList()
        domainEvents.clear()
        return events
    }
}
