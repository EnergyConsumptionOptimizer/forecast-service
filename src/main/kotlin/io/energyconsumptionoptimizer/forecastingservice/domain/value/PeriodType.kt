package io.energyconsumptionoptimizer.forecastingservice.domain.value

/**
 * Common aggregation periods used by the service.
 *
 * @property days number of days represented by the period
 */
enum class PeriodType(
    val days: Int,
) {
    ONE_DAY(1),
    ONE_WEEK(7),
    ONE_MONTH(30),
}
