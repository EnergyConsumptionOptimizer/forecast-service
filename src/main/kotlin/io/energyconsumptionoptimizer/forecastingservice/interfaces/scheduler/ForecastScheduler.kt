package io.energyconsumptionoptimizer.forecastingservice.interfaces.scheduler

import io.energyconsumptionoptimizer.forecastingservice.application.usecases.ComputeForecastUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

class ForecastScheduler(
    private val computeForecastUseCase: ComputeForecastUseCase,
    private val executionTime: LocalTime = LocalTime.of(0, 1),
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job =
            scope.launch {
                while (isActive) {
                    val delayMillis = millisUntilNextRun()
                    if (delayMillis > 0) {
                        delay(delayMillis)
                    }
                    runCatching { computeForecastUseCase.computeAll() }
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun millisUntilNextRun(): Long {
        val now = LocalDateTime.now()
        val nextRun = calculateNextExecutionTime(now)
        return Duration.between(now, nextRun).toMillis()
    }

    private fun calculateNextExecutionTime(now: LocalDateTime): LocalDateTime {
        val scheduledToday = now.toLocalDate().atTime(executionTime)
        return if (now.isBefore(scheduledToday)) scheduledToday else scheduledToday.plusDays(1)
    }
}
