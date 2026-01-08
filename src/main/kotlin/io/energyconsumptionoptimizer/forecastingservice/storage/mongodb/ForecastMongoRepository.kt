package io.energyconsumptionoptimizer.forecastingservice.storage.mongodb

import com.mongodb.client.model.ReplaceOptions
import io.energyconsumptionoptimizer.forecastingservice.domain.ForecastedConsumption
import io.energyconsumptionoptimizer.forecastingservice.domain.port.ForecastRepository
import io.energyconsumptionoptimizer.forecastingservice.domain.value.ForecastId
import io.energyconsumptionoptimizer.forecastingservice.domain.value.UtilityType
import io.energyconsumptionoptimizer.forecastingservice.storage.mongodb.document.ForecastedConsumptionDocument
import io.energyconsumptionoptimizer.forecastingservice.storage.mongodb.mapper.toDocument
import io.energyconsumptionoptimizer.forecastingservice.storage.mongodb.mapper.toDomain
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq

/**
 * Mongo-backed implementation of [ForecastRepository].
 *
 * Persists [ForecastedConsumption] entities into a MongoDB collection and
 * translates between domain objects and storage documents via the mapper helpers.
 *
 * @constructor Create a repository backed by `mongoClient` and targeting `databaseName`.
 * @param mongoClient KMongo [CoroutineClient] used to access the database.
 * @param databaseName Name of the MongoDB database (defaults to `forecast-service`).
 */
class ForecastMongoRepository(
    mongoClient: CoroutineClient,
    databaseName: String = "forecast-service",
) : ForecastRepository {
    private val collection: CoroutineCollection<ForecastedConsumptionDocument> =
        mongoClient
            .getDatabase(databaseName)
            .getCollection("forecasts")

    override suspend fun save(forecast: ForecastedConsumption): ForecastedConsumption {
        collection.replaceOne(
            ForecastedConsumptionDocument::utilityType eq forecast.utilityType.name,
            forecast.toDocument(),
            ReplaceOptions().upsert(true),
        )
        return forecast
    }

    override suspend fun findAll(): List<ForecastedConsumption> = collection.find().toList().map { it.toDomain() }

    override suspend fun findById(id: ForecastId): ForecastedConsumption? =
        collection.findOne(ForecastedConsumptionDocument::_id eq id.toString())?.toDomain()

    override suspend fun findByUtility(utilityType: UtilityType): ForecastedConsumption? =
        collection.findOne(ForecastedConsumptionDocument::utilityType eq utilityType.name)?.toDomain()

    override suspend fun remove(forecast: ForecastedConsumption): Boolean = removeById(forecast.id)

    override suspend fun removeById(id: ForecastId): Boolean =
        collection.deleteOne(ForecastedConsumptionDocument::_id eq id.toString()).deletedCount > 0

    override suspend fun removeByUtility(utilityType: UtilityType): Boolean =
        collection.deleteOne(ForecastedConsumptionDocument::utilityType eq utilityType.name).deletedCount > 0
}
