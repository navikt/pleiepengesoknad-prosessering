package no.nav.helse

import com.github.kittinunf.fuel.httpGet
import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.common.KafkaEnvironment
import no.nav.helse.dusseldorf.testsupport.jws.ClientCredentials
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2WellKnownUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsWellKnownUrl
import org.json.JSONObject

object TestConfiguration {

    fun asMap(
        wireMockServer: WireMockServer? = null,
        kafkaEnvironment: KafkaEnvironment? = null,
        port : Int = 8080,
        aktoerRegisterBaseUrl : String? = wireMockServer?.getAktoerRegisterBaseUrl(),
        tpsProxyBaseUrl : String? = wireMockServer?.getTpsProxyBaseUrl(),
        k9JoarkBaseUrl : String? = wireMockServer?.getK9JoarkBaseUrl(),
        k9DokumentBaseUrl : String? = wireMockServer?.getK9DokumentBaseUrl()
    ) : Map<String, String>{
        val map = mutableMapOf(
            Pair("ktor.deployment.port","$port"),
            Pair("nav.aktoer_register_base_url","$aktoerRegisterBaseUrl"),
            Pair("nav.tps_proxy_v1_base_url","$tpsProxyBaseUrl"),
            Pair("nav.k9_joark_base_url","$k9JoarkBaseUrl"),
            Pair("nav.k9_dokument_base_url","$k9DokumentBaseUrl")
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
            map["nav.auth.scopes.lagre-dokument"] = "k9-dokument/.default"
            map["nav.auth.scopes.slette-dokument"] = "k9-dokument/.default"
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
    private fun String.getAsJson() = JSONObject(this.httpGet().responseString().third.component1())
}
