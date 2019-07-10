package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.common.KafkaEnvironment

object TestConfiguration {

    fun asMap(
        wireMockServer: WireMockServer? = null,
        kafkaEnvironment: KafkaEnvironment? = null,
        port : Int = 8080,
        tokenUrl : String? = wireMockServer?.getTokenUrl(),
        authorizedSystems : String? = wireMockServer?.getSubject(),
        aktoerRegisterBaseUrl : String? = wireMockServer?.getAktoerRegisterBaseUrl(),
        pleiepengerOppgaveBaseUrl : String? = wireMockServer?.getPleiepengerOppgaveBaseUrl(),
        pleiepengerJoarkBaseUrl : String? = wireMockServer?.getPleiepengerJoarkBaseUrl(),
        pleiepeingerDokumentBaseUrl : String? = wireMockServer?.getPleiepengerDokumentBaseUrl(),
        clientSecret : String? = "foo"
    ) : Map<String, String>{
        val map = mutableMapOf(
            Pair("ktor.deployment.port","$port"),
            Pair("nav.auth.clients.0.alias", "nais-sts"),
            Pair("nav.auth.clients.0.client_id", "srvpps-prosessering"),
            Pair("nav.auth.clients.0.token_endpoint", "$tokenUrl"),
            Pair("nav.rest_api.authorized_systems","$authorizedSystems"),
            Pair("nav.aktoer_register_base_url","$aktoerRegisterBaseUrl"),
            Pair("nav.pleiepenger_oppgave_base_url","$pleiepengerOppgaveBaseUrl"),
            Pair("nav.pleiepenger_joark_base_url","$pleiepengerJoarkBaseUrl"),
            Pair("nav.pleiepenger_dokument_base_url","$pleiepeingerDokumentBaseUrl")
        )

        clientSecret?.let {
            map["nav.auth.clients.0.client_secret"] = it
        }

        kafkaEnvironment?.let {
            map["nav.kafka.bootstrap_servers"] = it.brokersURL
            map["nav.kafka.username"] = it.username()
            map["nav.kafka.password"] = it.password()
        }

        return map.toMap()
    }

    fun asArray(map : Map<String, String>) : Array<String>  {
        val list = mutableListOf<String>()
        map.forEach { configKey, configValue ->
            list.add("-P:$configKey=$configValue")
        }
        return list.toTypedArray()
    }
}