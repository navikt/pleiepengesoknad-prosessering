package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import java.util.*

private const val aktoerRegisterBasePath = "/aktoerregister-mock"
private const val pleiepengerOppgaveBaseUrl = "/pleiepenger-oppgave-mock"
private const val pleiepengerJoarkBaseUrl = "/pleiepenger-joark-mock"
private const val k9DokumentBasePath = "/k9-dokument-mock"

internal fun WireMockServer.stubAktoerRegisterGetAktoerIdNotFound(
    fnr: String) : WireMockServer{
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
    return this
}


internal fun WireMockServer.stubAktoerRegisterGetAktoerId(
    fnr: String,
    aktoerId: String) : WireMockServer {
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
    return this
}

internal fun WireMockServer.stubLagreDokument() : WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$k9DokumentBasePath.*")).willReturn(
            WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withHeader("Location", "${getK9DokumentBaseUrl()}/v1/dokument/${UUID.randomUUID()}")
                .withStatus(201)
        )
    )
    return this
}

internal fun WireMockServer.stubSlettDokument() : WireMockServer{
    WireMock.stubFor(
        WireMock.delete(WireMock.urlPathMatching(".*$k9DokumentBasePath.*")).willReturn(
            WireMock.aResponse()
                .withStatus(204)
        )
    )
    return this
}

internal fun WireMockServer.stubOpprettOppgave() : WireMockServer {
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
    return this
}

internal fun WireMockServer.stubJournalfor(responseCode: Int = 201) : WireMockServer {
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
    return this
}



private fun WireMockServer.stubHealthEndpoint(
    path : String
) : WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path")).willReturn(
            WireMock.aResponse()
                .withStatus(200)
        )
    )
    return this
}

internal fun WireMockServer.stubPleiepengerOppgaveHealth() = stubHealthEndpoint("$pleiepengerOppgaveBaseUrl/health")
internal fun WireMockServer.stubK9DokumentHealth() = stubHealthEndpoint("$k9DokumentBasePath/health")
internal fun WireMockServer.stubPleiepengerJoarkHealth() = stubHealthEndpoint("$pleiepengerJoarkBaseUrl/health")

internal fun WireMockServer.getAktoerRegisterBaseUrl() = baseUrl() + aktoerRegisterBasePath
internal fun WireMockServer.getPleiepengerOppgaveBaseUrl() = baseUrl() + pleiepengerOppgaveBaseUrl
internal fun WireMockServer.getPleiepengerJoarkBaseUrl() = baseUrl() + pleiepengerJoarkBaseUrl
internal fun WireMockServer.getK9DokumentBaseUrl() = baseUrl() + k9DokumentBasePath