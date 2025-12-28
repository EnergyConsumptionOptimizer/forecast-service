package io.energyconsumptionoptimizer.forecastingservice.utils.fakes

import io.energyconsumptionoptimizer.forecastingservice.domain.port.ForecastingAlgorithm
import io.energyconsumptionoptimizer.forecastingservice.domain.port.HistoricalData
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ConsumptionValue
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastedDataPoint
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class FakeForecastingAlgorithm : ForecastingAlgorithm {
    override val name: String = "FakeAlgorithm"

    override suspend fun forecast(
        historicalData: List<HistoricalData>,
        horizon: Int,
    ): List<ForecastedDataPoint> {
        val baseDate =
            Clock.System
                .now()
                .toLocalDateTime(TimeZone.UTC)
                .date
                .plus(1, DateTimeUnit.DAY)

        return (0 until horizon).map { dayOffset ->
            ForecastedDataPoint(
                date = baseDate.plus(dayOffset, DateTimeUnit.DAY),
                predictedValue = ConsumptionValue.of(100.0 + dayOffset),
            )
        }
    }
}
