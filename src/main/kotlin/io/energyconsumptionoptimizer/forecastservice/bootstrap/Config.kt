package io.energyconsumptionoptimizer.forecastservice.bootstrap

data class Config(
    val port: Int,
    val mongoUri: String,
    val mongoDatabase: String,
    val monitoringServiceUrl: String,
    val schedulerHour: Int,
    val schedulerMinute: Int,
) {
    companion object {
        fun fromEnv(): Config {
            val port = envInt("PORT", 3000)
            val mongoHost = env("MONGODB_HOST", "localhost")
            val mongoPort = envInt("MONGODB_PORT", 27017)
            val mongoDb = env("MONGO_DB", "forecast")
            val monitoringHost = env("MONITORING_SERVICE_HOST", "monitoring-service")
            val monitoringPort = envInt("MONITORING_SERVICE_PORT", 3000)
            val forecastHour = envInt("FORECAST_HOUR", 0)
            val forecastMinute = envInt("FORECAST_MINUTE", 0)

            validate(port, forecastHour, forecastMinute)

            return Config(
                port = port,
                mongoUri = "mongodb://$mongoHost:$mongoPort",
                mongoDatabase = mongoDb,
                monitoringServiceUrl = env("MONITORING_SERVICE_URL", "http://$monitoringHost:$monitoringPort"),
                schedulerHour = forecastHour,
                schedulerMinute = forecastMinute,
            )
        }

        private fun validate(
            port: Int,
            hour: Int,
            minute: Int,
        ) {
            require(port in 1..65535) { "PORT must be between 1 and 65535, got: $port" }
            require(hour in 0..23) { "FORECAST_HOUR must be between 0 and 23, got: $hour" }
            require(minute in 0..59) { "FORECAST_MINUTE must be between 0 and 59, got: $minute" }
        }

        private fun env(
            key: String,
            default: String,
        ): String = System.getenv(key) ?: default

        private fun envInt(
            key: String,
            default: Int,
        ): Int {
            val raw = System.getenv(key) ?: return default
            return requireNotNull(raw.toIntOrNull()) { "$key must be an integer, got: '$raw'" }
        }
    }
}
