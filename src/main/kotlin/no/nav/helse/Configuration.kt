package no.nav.helse

import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.dusseldorf.ktor.core.getOptionalList
import no.nav.helse.dusseldorf.ktor.core.getRequiredString
import java.net.URI

@KtorExperimentalAPI
data class Configuration(private val config : ApplicationConfig) {

    fun getAuthorizedSystemsForRestApi(): List<String> {
        return config.getOptionalList(
            key = "nav.rest_api.authorized_systems",
            builder = { value -> value},
            secret = false
        )
    }

    fun getIssuer() = config.getRequiredString("nav.authorization.issuer", secret = false)
    fun getJwksUrl() = URI(config.getRequiredString("nav.authorization.jwks_url", secret = false))

    fun getAktoerRegisterBaseUrl() = URI(config.getRequiredString("nav.aktoer_register_base_url", secret = false))
    fun getPleiepengerOppgaveBaseUrl() = URI(config.getRequiredString("nav.pleiepenger_oppgave_base_url", secret = false))
    fun getPleiepengerJoarkBaseUrl() = URI(config.getRequiredString("nav.pleiepenger_joark_base_url", secret = false))
    fun getPleiepengerDokumentBaseUrl() = URI(config.getRequiredString("nav.pleiepenger_dokument_base_url", secret = false))
}