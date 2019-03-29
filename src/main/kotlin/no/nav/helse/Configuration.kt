package no.nav.helse

import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.dusseldorf.ktor.core.getOptionalList
import no.nav.helse.dusseldorf.ktor.core.getRequiredString
import java.net.URL

@KtorExperimentalAPI
data class Configuration(private val config : ApplicationConfig) {

    fun getAuthorizedSystemsForRestApi(): List<String> {
        return config.getOptionalList(
            key = "nav.rest_api.authorized_systems",
            builder = { value -> value},
            secret = false
        )
    }

    fun getTokenUrl() : URL {
        return URL(config.getRequiredString("nav.authorization.token_url", secret = false))
    }

    fun getJwksUrl() : URL {
        return URL(config.getRequiredString("nav.authorization.jwks_url", secret = false))
    }

    fun getAktoerRegisterBaseUrl() : URL {
        return URL(config.getRequiredString("nav.aktoer_register_base_url", secret = false))
    }

    fun getOpprettOppgaveUrl() : URL {
        return URL(config.getRequiredString("nav.opprett_oppgave_url", secret = false))
    }

    fun getOpprettJournalPostUrl() : URL {
        return URL(config.getRequiredString("nav.opprett_journal_post_url", secret = false))
    }

    fun getPleiepengerDokumentBaseUrl() : URL {
        return URL(config.getRequiredString("nav.pleiepenger_dokument_base_url", secret = false))
    }

    fun getIssuer() : String {
        return config.getRequiredString("nav.authorization.issuer", secret = false)
    }

    fun getServiceAccountClientId(): String {
        return config.getRequiredString("nav.authorization.service_account.client_id", secret = false)
    }

    fun getServiceAccountClientSecret(): String {
        return config.getRequiredString(key = "nav.authorization.service_account.client_secret", secret = true)
    }

    fun getServiceAccountScopes(): List<String> {
        return config.getOptionalList(
            key = "nav.authorization.service_account.scopes",
            builder = { value -> value},
            secret = false
        )
    }
}