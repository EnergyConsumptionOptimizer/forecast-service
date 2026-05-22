package io.energyconsumptionoptimizer.forecastservice.infrastructure.scheduling

import arrow.core.raise.either
import io.energyconsumptionoptimizer.forecastservice.application.ApplicationError
import io.energyconsumptionoptimizer.forecastservice.application.ports.inbound.ComputeForecastService
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import io.energyconsumptionoptimizer.forecastservice.logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

class ForecastScheduler(
    private val computeForecastService: ComputeForecastService,
    private val schedulerHour: Int = 0,
    private val schedulerMinute: Int = 0,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var job: Job? = null
    private var firstRun = true
    private val logger = logger()

    fun start() {
        if (job?.isActive == true) return
        firstRun = true
        logger.info("Forecast scheduler started, will execute daily at {}:{}", schedulerHour, schedulerMinute)
        job =
            scope.launch {
                while (isActive) {
                    val delayMillis = millisUntilNextRun()
                    if (delayMillis > 0) {
                        delay(delayMillis.milliseconds)
                    }
                    try {
                        val result = either { computeForecastService.computeAll() }
                        result.fold(
                            ifLeft = { error ->
                                when (error) {
                                    is ApplicationError.BatchComputationFailed -> {
                                        logger.error(
                                            "Scheduled forecast had errors for {} utilities: {}",
                                            error.failures.size,
                                            error.failures,
                                        )
                                        logger.info(
                                            "Scheduled forecast completed, {} utilities processed ({} failed)",
                                            UtilityType.entries.size,
                                            error.failures.size,
                                        )
                                    }

                                    else -> {
                                        logger.error("Scheduled forecast failed with error: {}", error)
                                    }
                                }
                            },
                            ifRight = {
                                logger.info(
                                    "Scheduled forecast completed, {} utilities processed (0 failed)",
                                    UtilityType.entries.size,
                                )
                            },
                        )
                    } catch (
                        @Suppress("TooGenericExceptionCaught")
                        e: Exception,
                    ) {
                        logger.error("Scheduled forecast crashed with unexpected exception", e)
                    }
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
        logger.info("Forecast scheduler stopped")
    }

    private fun millisUntilNextRun(): Long {
        if (firstRun) {
            firstRun = false
            return 0
        }
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val nowEpoch = now.toEpochMilliseconds()
        val todayLocalDateTime = now.toLocalDateTime(timeZone)
        val scheduledToday =
            LocalDateTime(
                todayLocalDateTime.year,
                todayLocalDateTime.month,
                todayLocalDateTime.day,
                schedulerHour,
                schedulerMinute,
            )
        val scheduledEpoch = scheduledToday.toInstant(timeZone).toEpochMilliseconds()
        val millisInDay = 24 * 60 * 60 * 1000L
        return if (nowEpoch < scheduledEpoch) scheduledEpoch - nowEpoch else scheduledEpoch + millisInDay - nowEpoch
    }
}
