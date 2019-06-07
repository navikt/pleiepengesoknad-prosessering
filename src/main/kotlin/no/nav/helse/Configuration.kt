package no.nav.helse

import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.dusseldorf.ktor.auth.*
import no.nav.helse.dusseldorf.ktor.core.getOptionalList
import no.nav.helse.dusseldorf.ktor.core.getRequiredString
import java.net.URI

private const val NAIS_STS_ALIAS = "nais-sts"

@KtorExperimentalAPI
data class Configuration(private val config : ApplicationConfig) {

    private val issuers = config.issuers()

    init {
        if (issuers.isEmpty()) throw IllegalStateException("Må konfigureres opp minst en issuer.")
    }


    private fun getAuthorizedSystemsForRestApi(): List<String> {
        return config.getOptionalList(
            key = "nav.rest_api.authorized_systems",
            builder = { value -> value},
            secret = false
        )
    }

    fun issuers(): Map<Issuer, Set<ClaimRule>> {
        return config.issuers().withAdditionalClaimRules(
            mapOf(NAIS_STS_ALIAS to setOf(StandardClaimRules.Companion.EnforceSubjectOneOf(getAuthorizedSystemsForRestApi().toSet())))
        )
    }

    fun getAktoerRegisterBaseUrl() = URI(config.getRequiredString("nav.aktoer_register_base_url", secret = false))
    fun getPleiepengerOppgaveBaseUrl() = URI(config.getRequiredString("nav.pleiepenger_oppgave_base_url", secret = false))
    fun getPleiepengerJoarkBaseUrl() = URI(config.getRequiredString("nav.pleiepenger_joark_base_url", secret = false))
    fun getPleiepengerDokumentBaseUrl() = URI(config.getRequiredString("nav.pleiepenger_dokument_base_url", secret = false))
}