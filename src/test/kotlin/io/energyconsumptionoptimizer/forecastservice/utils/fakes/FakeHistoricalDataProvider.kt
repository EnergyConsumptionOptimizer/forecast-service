package io.energyconsumptionoptimizer.forecastservice.utils.fakes

import io.energyconsumptionoptimizer.forecastservice.domain.port.HistoricalData
import io.energyconsumptionoptimizer.forecastservice.domain.port.HistoricalDataProvider
import io.energyconsumptionoptimizer.forecastservice.domain.value.ConsumptionValue
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class FakeHistoricalDataProvider : HistoricalDataProvider {
    override suspend fun fetchAggregatedHistoricalData(utilityType: UtilityType): List<HistoricalData> {
        val today =
            Clock.System
                .now()
                .toLocalDateTime(TimeZone.UTC)
                .date

        return (1..70).map { daysAgo ->
            HistoricalData(
                timestamp = today.minus(daysAgo, DateTimeUnit.DAY),
                value = ConsumptionValue.of(100.0 + daysAgo),
            )
        }
    }
}
