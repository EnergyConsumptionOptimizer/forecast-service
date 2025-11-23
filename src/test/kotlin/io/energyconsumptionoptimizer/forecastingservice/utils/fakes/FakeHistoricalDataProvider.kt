package io.energyconsumptionoptimizer.forecastingservice.utils.fakes

import io.energyconsumptionoptimizer.forecastingservice.domain.port.HistoricalData
import io.energyconsumptionoptimizer.forecastingservice.domain.port.HistoricalDataProvider
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ConsumptionValue
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType
import java.time.LocalDate

class FakeHistoricalDataProvider : HistoricalDataProvider {
    override suspend fun fetchAggregatedHistoricalData(utilityType: UtilityType): List<HistoricalData> {
        val today = LocalDate.now()
        return (1..70).map {
            HistoricalData(
                timestamp = today.minusDays(it.toLong()),
                value = ConsumptionValue.of(100.0 + it),
            )
        }
    }
}
