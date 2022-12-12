package no.nav.helse
import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.helse.dusseldorf.testsupport.jws.ClientCredentials
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2WellKnownUrl
import org.testcontainers.containers.KafkaContainer

object TestConfiguration {

    fun asMap(
        wireMockServer: WireMockServer? = null,
        kafkaContainer: KafkaContainer? = null,
        port : Int = 8080,
        k9JoarkBaseUrl : String? = wireMockServer?.getK9JoarkBaseUrl(),
        k9MellomlagringBaseUrl : String? = wireMockServer?.getK9MellomlagringBaseUrl()
    ) : Map<String, String>{
        val map = mutableMapOf(
            Pair("ktor.deployment.port","$port"),
            Pair("nav.k9_joark_base_url","$k9JoarkBaseUrl"),
            Pair("nav.k9_mellomlagring_base_url","$k9MellomlagringBaseUrl")
        )

        // Clients
        if (wireMockServer != null) {
            map["nav.auth.clients.1.alias"] = "azure-v2"
            map["nav.auth.clients.1.client_id"] = "pleiepengesoknad-prosessering"
            map["nav.auth.clients.1.private_key_jwk"] = ClientCredentials.ClientA.privateKeyJwk
            map["nav.auth.clients.1.certificate_hex_thumbprint"] = ClientCredentials.ClientA.certificateHexThumbprint
            map["nav.auth.clients.1.discovery_endpoint"] = wireMockServer.getAzureV2WellKnownUrl()
            map["nav.auth.scopes.journalfore"] = "pleiepenger-joark/.default"
            map["nav.auth.scopes.k9_mellomlagring"] = "k9-mellomlagring/.default"
        }

        kafkaContainer?.let {
            map["nav.kafka.bootstrap_servers"] = it.bootstrapServers
            map["nav.kafka.soknad_auto_offset_reset"] = "earliest"
            map["nav.kafka.endringsmelding_auto_offset_reset"] = "earliest"
        }

        return map.toMap()
    }
}
