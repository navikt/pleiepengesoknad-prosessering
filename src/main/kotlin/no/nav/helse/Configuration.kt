package no.nav.helse

import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL

private val logger: Logger = LoggerFactory.getLogger("nav.Configuration")

@KtorExperimentalAPI
data class Configuration(private val config : ApplicationConfig) {
    private fun getString(key: String,
                          secret: Boolean,
                          optional: Boolean) : String? {
        val configValue = config.propertyOrNull(key) ?: return if (optional) null else throw IllegalArgumentException("$key må settes.")
        val stringValue = configValue.getString()
        if (stringValue.isBlank()) {
            return if (optional) null else throw IllegalArgumentException("$key må settes.")
        }
        logger.info("{}={}", key, if (secret) "***" else stringValue)
        return stringValue
    }
    private fun getRequiredString(key: String, secret: Boolean = false) : String = getString(key, secret, false)!!
    private fun getOptionalString(key: String, secret: Boolean = false) : String? = getString(key, secret, true)
    private fun <T>getListFromCsv(csv: String, builder: (value: String) -> T) : List<T> = csv.replace(" ", "").split(",").map(builder)

    fun getAuthorizedSystemsForRestApi(): List<String> {
        val csv = getOptionalString("nav.rest_api.authorized_systems") ?: return emptyList()
        return getListFromCsv(
            csv = csv,
            builder = { value -> value}
        )
    }

    fun getTokenUrl() : URL {
        return URL(getRequiredString("nav.authorization.token_url"))
    }

    fun getJwksUrl() : URL {
        return URL(getRequiredString("nav.authorization.jwks_url"))
    }

    fun getAktoerRegisterBaseUrl() : URL {
        return URL(getRequiredString("nav.aktoer_register_base_url"))
    }

    fun getOpprettOppgaveUrl() : URL {
        return URL(getRequiredString("nav.opprett_oppgave_url"))
    }

    fun getOpprettJournalPostUrl() : URL {
        return URL(getRequiredString("nav.opprett_journal_post_url"))
    }

    fun getPleiepengerDokumentBaseUrl() : URL {
        return URL(getRequiredString("nav.pleiepenger_dokument_base_url"))
    }

    fun getIssuer() : String {
        return getRequiredString("nav.authorization.issuer")
    }

    fun getServiceAccountClientId(): String {
        return getRequiredString("nav.authorization.service_account.client_id")
    }

    fun getServiceAccountClientSecret(): String {
        return getRequiredString(key = "nav.authorization.service_account.client_secret", secret = true)
    }

    fun getServiceAccountScopes(): List<String> {
        val csv = getOptionalString("nav.authorization.service_account.scopes") ?: return emptyList()

        return getListFromCsv(
            csv = csv,
            builder = { value -> value}
        )
    }

    fun logIndirectlyUsedConfiguration() {
        logger.info("# Indirectly used configuration")
        val properties = System.getProperties()
        logger.info("## System Properties")
        properties.forEach { key, value ->
            if (key is String && (key.startsWith(prefix = "http", ignoreCase = true) || key.startsWith(prefix = "https", ignoreCase = true))) {
                logger.info("$key=$value")
            }
        }
        logger.info("## Environment variables")
        val environmentVariables = System.getenv()
        logger.info("HTTP_PROXY=${environmentVariables["HTTP_PROXY"]}")
        logger.info("HTTPS_PROXY=${environmentVariables["HTTPS_PROXY"]}")
        logger.info("NO_PROXY=${environmentVariables["NO_PROXY"]}")
    }
}