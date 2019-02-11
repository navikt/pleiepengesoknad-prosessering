package no.nav.helse

import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("nav.Configuration")

@KtorExperimentalAPI
data class Configuration(private val config : ApplicationConfig) {
    private fun getString(key: String,
                          secret: Boolean = false) : String  {
        val stringValue = config.property(key).getString()
        logger.info("{}={}", key, if (secret) "***" else stringValue)
        return stringValue
    }

    private fun <T>getListFromCsv(key: String,
                                  secret: Boolean = false,
                                  builder: (value: String) -> T) : List<T> {
        val csv = getString(key, false)
        val list = csv.replace(" ", "").split(",")
        val builtList = mutableListOf<T>()
        list.forEach { entry ->
            logger.info("$key entry = ${if (secret) "***" else entry}")
            builtList.add(builder(entry))
        }
        return builtList.toList()
    }

    fun getAuthorizedSystemsForRestApi(): List<String> {
        return getListFromCsv(
            key = "nav.rest_api.authorized_systems",
            builder = { value -> value}
        )
    }

}