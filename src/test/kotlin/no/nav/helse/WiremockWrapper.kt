package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.Extension
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import no.nav.security.oidc.test.support.JwkGenerator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

private val logger: Logger = LoggerFactory.getLogger("nav.WiremockWrapper")
private const val jwkSetPath = "/auth-mock/jwk-set"
private const val tokenPath = "/auth-mock/token"
private const val subject = "srvpleiepengesokna"

private const val aktoerRegisterBasePath = "/aktoerregister-mock"
private const val pleiepengerOppgaveBaseUrl = "/pleiepenger-oppgave-mock"
private const val pleiepengerJoarkBaseUrl = "/pleiepenger-joark-mock"
private const val pleiepengerDokumentBasePath = "/pleiepenger-dokument-mock"

object WiremockWrapper {

    fun bootstrap(
        port: Int? = null,
        extensions : Array<Extension> = arrayOf()) : WireMockServer {

        val wireMockConfiguration = WireMockConfiguration.options()

        extensions.forEach {
            wireMockConfiguration.extensions(it)
        }

        if (port == null) {
            wireMockConfiguration.dynamicPort()
        } else {
            wireMockConfiguration.port(port)
        }

        val wireMockServer = WireMockServer(wireMockConfiguration)

        wireMockServer.start()
        WireMock.configureFor(wireMockServer.port())

        stubGetSystembrukerToken()
        stubJwkSet()

        stubHealthEndpoint("$pleiepengerOppgaveBaseUrl/health")
        stubHealthEndpoint("$pleiepengerDokumentBasePath/health")
        stubHealthEndpoint("$pleiepengerJoarkBaseUrl/health")

        stubJournalfor()
        stubOpprettOppgave()
        stubLagreDokument(wireMockServer.getPleiepengerDokumentBaseUrl())
        stubSlettDokument()
        stubAktoerRegisterGetAktoerId("29099012345", "123456")


        logger.info("Mock available on '{}'", wireMockServer.baseUrl())
        return wireMockServer
    }

    private fun stubGetSystembrukerToken() {
        WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching(".*$tokenPath.*"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "access_token": "i-am-a-access-token",
                                "token_type": "Bearer",
                                "expires_in": 1000
                            }
                        """.trimIndent())
                )
        )
    }

    private fun stubJwkSet() {
        WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching(".*$jwkSetPath.*"))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(WiremockWrapper::class.java.getResource(JwkGenerator.DEFAULT_JWKSET_FILE).readText())
                )
        )
    }

    fun stubAktoerRegisterGetAktoerIdNotFound(
        fnr: String) {
        WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching(".*$aktoerRegisterBasePath/.*")).withHeader("Nav-Personidenter", EqualToPattern(fnr)).willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                    {
                      "$fnr": {
                        "identer": null,
                        "feilmelding": "Den angitte personidenten finnes ikke"
                      }
                    }
                    """.trimIndent())
                    .withStatus(200)
            )
        )
    }


    fun stubAktoerRegisterGetAktoerId(
        fnr: String,
        aktoerId: String) {
        WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching(".*$aktoerRegisterBasePath/.*")).withHeader("Nav-Personidenter", EqualToPattern(fnr)).willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                    {
                      "$fnr": {
                        "identer": [
                          {
                            "ident": "$aktoerId",
                            "identgruppe": "AktoerId",
                            "gjeldende": true
                          }
                        ],
                        "feilmelding": null
                      }
                    }
                    """.trimIndent())
                    .withStatus(200)
            )
        )
    }

    private fun stubLagreDokument(baseUrl : String) {
        WireMock.stubFor(
            WireMock.post(WireMock.urlPathMatching(".*$pleiepengerDokumentBasePath.*")).willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withHeader("Location", "$baseUrl/v1/dokument/${UUID.randomUUID()}")
                    .withStatus(201)
            )
        )
    }

    private fun stubSlettDokument() {
        WireMock.stubFor(
            WireMock.delete(WireMock.urlPathMatching(".*$pleiepengerDokumentBasePath.*")).willReturn(
                WireMock.aResponse()
                    .withStatus(204)
            )
        )
    }

    private fun stubOpprettOppgave() {
        WireMock.stubFor(
            WireMock.post(WireMock.urlPathMatching(".*$pleiepengerOppgaveBaseUrl.*")).willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                    {
                        "oppgave_id" : "5678"
                    }
                    """.trimIndent())
                    .withStatus(201)
            )
        )
    }

    fun stubJournalfor(responseCode: Int = 201) {
        WireMock.stubFor(
            WireMock.post(WireMock.urlPathMatching(".*$pleiepengerJoarkBaseUrl.*")).willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                    {
                        "journal_post_id" : "9101112"
                    }
                    """.trimIndent())
                    .withStatus(responseCode)
            )
        )
    }

    private fun stubHealthEndpoint(
        path : String
    ) {
        WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching(".*$path")).willReturn(
                WireMock.aResponse()
                    .withStatus(200)
            )
        )
    }
}

fun WireMockServer.getJwksUrl() : String {
    return baseUrl() + jwkSetPath
}

fun WireMockServer.getTokenUrl() : String {
    return baseUrl() + tokenPath
}

fun WireMockServer.getAktoerRegisterBaseUrl() : String {
    return baseUrl() + aktoerRegisterBasePath
}

fun WireMockServer.getPleiepengerOppgaveBaseUrl() : String {
    return baseUrl() + pleiepengerOppgaveBaseUrl
}

fun WireMockServer.getPleiepengerJoarkBaseUrl() : String {
    return baseUrl() + pleiepengerJoarkBaseUrl
}

fun WireMockServer.getPleiepengerDokumentBaseUrl() : String {
    return baseUrl() + pleiepengerDokumentBasePath
}

fun WireMockServer.getSubject() : String {
    return subject
}