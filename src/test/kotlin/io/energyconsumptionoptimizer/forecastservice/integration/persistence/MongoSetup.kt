package io.energyconsumptionoptimizer.forecastservice.integration.persistence

import de.flapdoodle.embed.mongo.commands.MongodArguments
import de.flapdoodle.embed.mongo.config.Net
import de.flapdoodle.embed.mongo.config.Storage
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.transitions.Mongod
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess
import de.flapdoodle.embed.process.io.ProcessOutput
import de.flapdoodle.reverse.TransitionWalker
import de.flapdoodle.reverse.transitions.Start
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

object MongoSetup {
    private val logger = LoggerFactory.getLogger(javaClass)
    private const val PORT = 27018
    private const val HOST = "localhost"
    const val CONNECTION_STRING = "mongodb://$HOST:$PORT"

    private var mongodProcess: RunningMongodProcess? = null
    var available: Boolean = false
        private set

    fun start() {
        if (mongodProcess != null) return

        try {
            logger.info("Starting embedded MongoDB on port {}", PORT)

            val mongodArgs =
                MongodArguments
                    .builder()
                    .replication(Storage.of("rs0", 0))
                    .build()

            val mongod =
                Mongod
                    .builder()
                    .net(Start.to(Net::class.java).initializedWith(Net.of(HOST, PORT, false)))
                    .processOutput(
                        Start
                            .to(ProcessOutput::class.java)
                            .initializedWith(ProcessOutput.silent()),
                    ).mongodArguments(
                        Start
                            .to(MongodArguments::class.java)
                            .initializedWith(mongodArgs),
                    ).build()

            val reached: TransitionWalker.ReachedState<RunningMongodProcess> =
                mongod.start(Version.Main.PRODUCTION)

            mongodProcess = reached.current()
            initReplicaSet()
            available = true
            logger.info("Embedded MongoDB started on {}", CONNECTION_STRING)
        } catch (e: Exception) {
            available = false
            logger.warn(
                "Embedded MongoDB not available (libssl1.1 required). " +
                    "Integration tests will be skipped. Error: {}",
                e.message,
            )
        }
    }

    private fun initReplicaSet() {
        runBlocking {
            val client = KMongo.createClient(CONNECTION_STRING).coroutine
            try {
                val adminDb = client.getDatabase("admin")
                val config =
                    org.bson
                        .Document()
                        .append("_id", "rs0")
                        .append(
                            "members",
                            listOf(
                                org.bson
                                    .Document()
                                    .append("_id", 0)
                                    .append("host", "$HOST:$PORT"),
                            ),
                        )
                adminDb.runCommand<org.bson.Document>(
                    org.bson.Document("replSetInitiate", config),
                )
                delay(2000.milliseconds)
                logger.info("Replica set rs0 initiated")
            } catch (e: Exception) {
                logger.warn("Replica set init may have already occurred: {}", e.message)
            } finally {
                client.close()
            }
        }
    }

    fun stop() {
        mongodProcess?.stop()
        mongodProcess = null
        available = false
        logger.info("Embedded MongoDB stopped")
    }

    fun createClient(): CoroutineClient = KMongo.createClient(CONNECTION_STRING).coroutine

    suspend fun clearDatabase(
        client: CoroutineClient,
        databaseName: String,
    ) {
        val db = client.getDatabase(databaseName)
        db
            .listCollectionNames()
            .toList()
            .forEach { collectionName ->
                db.getCollection<org.bson.Document>(collectionName).drop()
            }
    }
}
