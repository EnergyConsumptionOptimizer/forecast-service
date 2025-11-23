package io.energyconsumptionoptimizer.forecastingservice.domain.value

enum class PeriodType(
    val days: Int,
) {
    ONE_DAY(1),
    ONE_WEEK(7),
    ONE_MONTH(30),
}
