package io.energyconsumptionoptimizer.forecastservice.infrastructure.persistence.mongo

import com.mongodb.client.model.ReplaceOptions
import io.energyconsumptionoptimizer.forecastservice.domain.entity.Forecast
import io.energyconsumptionoptimizer.forecastservice.domain.value.UtilityType
import io.energyconsumptionoptimizer.forecastservice.logger
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq

class MongoForecastRepository(
    mongoClient: CoroutineClient,
    databaseName: String,
) : io.energyconsumptionoptimizer.forecastservice.domain.ports.ForecastRepository {
    private val collection: CoroutineCollection<ForecastDocument> =
        mongoClient
            .getDatabase(databaseName)
            .getCollection("forecasts")

    private val logger = logger()

    override suspend fun save(forecast: Forecast) {
        val doc = ForecastMapper.toDocument(forecast)
        val session = currentMongoSession()
        if (session != null) {
            collection.replaceOne(
                clientSession = session,
                filter = ForecastDocument::utilityType eq doc.utilityType,
                replacement = doc,
                options = ReplaceOptions().upsert(true),
            )
        } else {
            collection.replaceOne(
                ForecastDocument::utilityType eq doc.utilityType,
                doc,
                ReplaceOptions().upsert(true),
            )
        }
        logger.debug("Saved forecast utilityType={}", forecast.utilityType.name)
    }

    override suspend fun findAll(): List<Forecast> {
        val docs = collection.find().toList()
        return docs.mapNotNull { ForecastMapper.toDomain(it) }
    }

    override suspend fun findByUtility(utilityType: UtilityType): Forecast? {
        val doc = collection.findOne(ForecastDocument::utilityType eq utilityType.name)
        return doc?.let { ForecastMapper.toDomain(it) }
    }

    override suspend fun remove(forecast: Forecast) {
        val session = currentMongoSession()
        if (session != null) {
            collection.deleteOne(
                clientSession = session,
                filter = ForecastDocument::utilityType eq forecast.utilityType.name,
            )
        } else {
            collection.deleteOne(ForecastDocument::utilityType eq forecast.utilityType.name)
        }
        logger.debug("Removed forecast utilityType={}", forecast.utilityType.name)
    }
}
