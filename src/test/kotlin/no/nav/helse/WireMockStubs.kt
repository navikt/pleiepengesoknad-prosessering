package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import io.ktor.http.HttpHeaders
import no.nav.helse.dusseldorf.ktor.testsupport.wiremock.WireMockBuilder
import java.util.*

private const val aktoerRegisterBasePath = "/aktoerregister-mock"
private const val tpsProxyBasePath = "/tps-proxy-mock"
private const val k9JoarkBaseUrl = "/k9-joark-mock"
private const val k9DokumentBasePath = "/k9-dokument-mock"

fun WireMockBuilder.navnOppslagConfig() = wireMockConfiguration {

}

internal fun WireMockServer.stubAktoerRegisterGetAktoerIdNotFound(
    fnr: String
): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$aktoerRegisterBasePath/.*")).withHeader(
            "Nav-Personidenter",
            EqualToPattern(fnr)
        ).willReturn(
            WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(
                    """
                    {
                      "$fnr": {
                        "identer": null,
                        "feilmelding": "Den angitte personidenten finnes ikke"
                      }
                    }
                    """.trimIndent()
                )
                .withStatus(200)
        )
    )
    return this
}


internal fun WireMockServer.stubAktoerRegister(
    identNummer: String,
    aktoerId: String
): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$aktoerRegisterBasePath/.*"))
            .withQueryParam("gjeldende", EqualToPattern("true"))
            .withQueryParam("identgruppe", EqualToPattern("AktoerId"))
            .withHeader("Nav-Personidenter", EqualToPattern(identNummer))
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                    {
                      "$identNummer": {
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
                    """.trimIndent()
                    )
                    .withStatus(200)
            )
    )

    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$aktoerRegisterBasePath/.*"))
            .withQueryParam("gjeldende", EqualToPattern("true"))
            .withQueryParam("identgruppe", EqualToPattern("NorskIdent"))
            .withHeader("Nav-Personidenter", EqualToPattern(aktoerId))
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(
                        """
                        {
                          "$aktoerId": {
                            "identer": [
                              {
                                "ident": "$identNummer",
                                "identgruppe": "NorskIdent",
                                "gjeldende": true
                              }
                            ],
                            "feilmelding": null
                          }
                        }
                        """.trimIndent()
                    )
            )
    )
    return this
}



internal fun WireMockServer.stubLagreDokument(): WireMockServer {
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

internal fun WireMockServer.stubSlettDokument(): WireMockServer {
    WireMock.stubFor(
        WireMock.delete(WireMock.urlPathMatching(".*$k9DokumentBasePath.*")).willReturn(
            WireMock.aResponse()
                .withStatus(204)
        )
    )
    return this
}

internal fun WireMockServer.stubJournalfor(responseCode: Int = 201): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$k9JoarkBaseUrl.*")).willReturn(
            WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(
                    """
                    {
                        "journal_post_id" : "9101112"
                    }
                    """.trimIndent()
                )
                .withStatus(responseCode)
        )
    )
    return this
}

internal fun WireMockServer.stubTpsProxyGetNavn(fornavn: String, mellomNavn: String? = "BLUNKENDE", etterNavn: String): WireMockServer {
    val mellomnavnUtenNull = if (mellomNavn.isNullOrBlank()) null else "$mellomNavn"
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$tpsProxyBasePath/navn"))
            .withHeader(HttpHeaders.Authorization, AnythingPattern())
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(
                        """  
                            {
                                "fornavn": "$fornavn",
                                "mellomnavn": $mellomnavnUtenNull,
                                "etternavn": "$etterNavn"
                            }
                        """.trimIndent()
                    )
            )
    )
    return this
}

private fun WireMockServer.stubHealthEndpoint(
    path: String
): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path")).willReturn(
            WireMock.aResponse()
                .withStatus(200)
        )
    )
    return this
}

internal fun WireMockServer.stubK9DokumentHealth() = stubHealthEndpoint("$k9DokumentBasePath/health")
internal fun WireMockServer.stubPleiepengerJoarkHealth() = stubHealthEndpoint("$k9JoarkBaseUrl/health")

internal fun WireMockServer.getAktoerRegisterBaseUrl() = baseUrl() + aktoerRegisterBasePath
internal fun WireMockServer.getTpsProxyBaseUrl() = baseUrl() + tpsProxyBasePath
internal fun WireMockServer.getK9JoarkBaseUrl() = baseUrl() + k9JoarkBaseUrl
internal fun WireMockServer.getK9DokumentBaseUrl() = baseUrl() + k9DokumentBasePath
