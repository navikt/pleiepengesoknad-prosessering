package no.nav.helse

import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.dusseldorf.ktor.core.getOptionalString
import no.nav.helse.dusseldorf.ktor.core.getRequiredString
import no.nav.helse.kafka.KafkaConfig
import java.net.URI
import java.time.Duration
import java.time.temporal.ChronoUnit

private const val NAIS_STS_ALIAS = "nais-sts"

@KtorExperimentalAPI
data class Configuration(private val config : ApplicationConfig) {

    fun getAktoerRegisterBaseUrl() = URI(config.getRequiredString("nav.aktoer_register_base_url", secret = false))
    fun getPleiepengerOppgaveBaseUrl() = URI(config.getRequiredString("nav.pleiepenger_oppgave_base_url", secret = false))
    fun getPleiepengerJoarkBaseUrl() = URI(config.getRequiredString("nav.pleiepenger_joark_base_url", secret = false))
    fun getPleiepengerDokumentBaseUrl() = URI(config.getRequiredString("nav.pleiepenger_dokument_base_url", secret = false))


    private fun unreadyAfterStreamStoppedIn() = Duration.of(
        config.getRequiredString("nav.kafka.unready_after_stream_stopped_in.amount", secret = false).toLong(),
        ChronoUnit.valueOf(config.getRequiredString("nav.kafka.unready_after_stream_stopped_in.unit", secret = false))
    )

    internal fun getKafkaConfig() = config.getRequiredString("nav.kafka.bootstrap_servers", secret = false).let { bootstrapServers ->
        val trustStore = config.getOptionalString("nav.trust_store.path", secret = false)?.let { trustStorePath ->
            config.getOptionalString("nav.trust_store.password", secret = true)?.let { trustStorePassword ->
                Pair(trustStorePath, trustStorePassword)
            }
        }

        KafkaConfig(
            bootstrapServers = bootstrapServers,
            credentials = Pair(config.getRequiredString("nav.kafka.username", secret = false), config.getRequiredString("nav.kafka.password", secret = true)),
            trustStore = trustStore,
            unreadyAfterStreamStoppedIn = unreadyAfterStreamStoppedIn()
        )
    }
}