package io.energyconsumptionoptimizer.forecastingservice.utils.fakes

import io.energyconsumptionoptimizer.forecastingservice.domain.port.ForecastingAlgorithm
import io.energyconsumptionoptimizer.forecastingservice.domain.port.HistoricalData
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ConsumptionValue
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastedDataPoint
import java.time.LocalDate

class FakeForecastingAlgorithm : ForecastingAlgorithm {
    override val name: String = "FakeAlgorithm"

    override suspend fun forecast(
        historicalData: List<HistoricalData>,
        horizon: Int,
    ): List<ForecastedDataPoint> {
        val baseDate = LocalDate.now().plusDays(1)
        return (0 until horizon).map { dayOffset ->
            ForecastedDataPoint(
                date = baseDate.plusDays(dayOffset.toLong()),
                predictedValue = ConsumptionValue.of(100.0 + dayOffset),
            )
        }
    }
}
