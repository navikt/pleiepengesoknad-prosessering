package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.common.KafkaEnvironment
import no.nav.helse.dusseldorf.ktor.testsupport.jws.ClientCredentials
import no.nav.helse.dusseldorf.ktor.testsupport.wiremock.getAzureV2WellKnownUrl
import no.nav.helse.dusseldorf.ktor.testsupport.wiremock.getNaisStsWellKnownUrl

object TestConfiguration {

    fun asMap(
        wireMockServer: WireMockServer? = null,
        kafkaEnvironment: KafkaEnvironment? = null,
        port : Int = 8080,
        aktoerRegisterBaseUrl : String? = wireMockServer?.getAktoerRegisterBaseUrl(),
        pleiepengerOppgaveBaseUrl : String? = wireMockServer?.getPleiepengerOppgaveBaseUrl(),
        pleiepengerJoarkBaseUrl : String? = wireMockServer?.getPleiepengerJoarkBaseUrl(),
        pleiepeingerDokumentBaseUrl : String? = wireMockServer?.getPleiepengerDokumentBaseUrl()
    ) : Map<String, String>{
        val map = mutableMapOf(
            Pair("ktor.deployment.port","$port"),
            Pair("nav.aktoer_register_base_url","$aktoerRegisterBaseUrl"),
            Pair("nav.pleiepenger_oppgave_base_url","$pleiepengerOppgaveBaseUrl"),
            Pair("nav.pleiepenger_joark_base_url","$pleiepengerJoarkBaseUrl"),
            Pair("nav.pleiepenger_dokument_base_url","$pleiepeingerDokumentBaseUrl")
        )

        // Clients
        if (wireMockServer != null) {
            map["nav.auth.clients.0.alias"] = "nais-sts"
            map["nav.auth.clients.0.client_id"] = "srvpps-prosessering"
            map["nav.auth.clients.0.client_secret"] = "very-secret"
            map["nav.auth.clients.0.discovery_endpoint"] = wireMockServer.getNaisStsWellKnownUrl()
        }

        if (wireMockServer != null) {
            map["nav.auth.clients.1.alias"] = "azure-v2"
            map["nav.auth.clients.1.client_id"] = "pleiepengesoknad-prosessering"
            map["nav.auth.clients.1.private_key_jwk"] = ClientCredentials.ClientA.privateKeyJwk
            map["nav.auth.clients.1.certificate_hex_thumbprint"] = ClientCredentials.ClientA.certificateHexThumbprint
            map["nav.auth.clients.1.discovery_endpoint"] = wireMockServer.getAzureV2WellKnownUrl()
            map["nav.auth.scopes.lagre-dokument"] = "pleiepenger-dokument/.default"
            map["nav.auth.scopes.slette-dokument"] = "pleiepenger-dokument/.default"
            map["nav.auth.scopes.journalfore"] = "pleiepenger-joark/.default"
            map["nav.auth.scopes.opprette-oppgave"] = "pleiepenger-oppgave/.default"
        }

        kafkaEnvironment?.let {
            map["nav.kafka.bootstrap_servers"] = it.brokersURL
            map["nav.kafka.username"] = it.username()
            map["nav.kafka.password"] = it.password()
        }

        return map.toMap()
    }
}