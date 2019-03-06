package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.Extension
import no.nav.helse.gosys.JournalPostId
import no.nav.security.oidc.test.support.JwkGenerator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("nav.WiremockWrapper")
private const val jwkSetPath = "/auth-mock/jwk-set"
private const val tokenPath = "/auth-mock/token"
private const val getAccessTokenPath = "/auth-mock/get-test-access-token"
private const val subject = "srvpleiepengesokna"

private const val aktoerRegisterBasePath = "/aktoerregister-mock"
private const val opprettSakPath = "/pleiepenger-sak-mock/v1/sak"
private const val opprettOppgavePath = "/pleiepenger-oppgave-mock/v1/oppgave"
private const val opprettJournalPostPath = "/pleiepenger-joark-mock/v1/journalforing"
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

        provideGetAccessTokenEndPoint(wireMockServer.baseUrl())

        stubOpprettSak()
        stubJournalfor()
        stubOpprettOppgave()
        stubLagreDokument()
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
                        .withBody("{\"access_token\":\"i-am-an-access-token\", \"expires_in\": 5000}")
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

    private fun provideGetAccessTokenEndPoint(issuer: String) {
        val jwt = Authorization.getAccessToken(issuer, subject)
        WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching(".*$getAccessTokenPath.*"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"$jwt\", \"expires_in\": 5000}")
                )
        )
    }


    fun stubAktoerRegisterGetAktoerId(
        fnr: String,
        aktoerId: String) {
        WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching(".*$aktoerRegisterBasePath/.*")).willReturn(
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

    fun stubLagreDokument() {
        WireMock.stubFor(
            WireMock.post(WireMock.urlPathMatching(".*$pleiepengerDokumentBasePath.*")).willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withHeader("Location", "http://localhost:1337/dokument")
                    .withStatus(201)
            )
        )
    }

    fun stubOpprettSak() {
        WireMock.stubFor(
            WireMock.post(WireMock.urlPathMatching(".*$opprettSakPath")).willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                    {
                        "sak_id" : "1234"
                    }
                    """.trimIndent())
                    .withStatus(201)
            )
        )
    }

    fun stubOpprettOppgave() {
        WireMock.stubFor(
            WireMock.post(WireMock.urlPathMatching(".*$opprettOppgavePath")).willReturn(
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

    fun stubJournalfor() {
        WireMock.stubFor(
            WireMock.post(WireMock.urlPathMatching(".*$opprettJournalPostPath")).willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                    {
                        "journal_post_id" : "9101112"
                    }
                    """.trimIndent())
                    .withStatus(201)
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

fun WireMockServer.getOpprettSakUrl() : String {
    return baseUrl() + opprettSakPath
}

fun WireMockServer.getOpprettOppgaveUrl() : String {
    return baseUrl() + opprettOppgavePath
}

fun WireMockServer.getOpprettJournalPostUrl() : String {
    return baseUrl() + opprettJournalPostPath
}

fun WireMockServer.getPleiepengerDokumentBaseUrl() : String {
    return baseUrl() + pleiepengerDokumentBasePath
}

fun WireMockServer.getSubject() : String {
    return subject
}