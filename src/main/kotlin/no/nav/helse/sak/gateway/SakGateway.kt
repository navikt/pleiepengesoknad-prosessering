package no.nav.helse.sak.gateway

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.prometheus.client.Histogram
import no.nav.helse.HttpRequest
import no.nav.helse.sak.CorrelationId
import no.nav.helse.systembruker.SystembrukerService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL

private val logger: Logger = LoggerFactory.getLogger("nav.SakGateway")

private val nySak = Histogram.build(
    "histogram_ny_sak",
    "Tidsbruk for ny sak mot Sak"
).register()

/*
    https://sak.nais.preprod.local/?url=https://sak.nais.preprod.local/api/swagger.json#/v1saker/hentSak
 */

class SakGateway(
    private val httpClient: HttpClient,
    sakBaseUrl: URL,
    private val systembrukerService: SystembrukerService
) {

    private val opprettSakUrl : URL = HttpRequest.buildURL(sakBaseUrl, pathParts = listOf("api", "v1", "saker"))

    internal suspend fun opprettSak(
        request: OpprettSakRequest,
        correlationId: CorrelationId
    ) : OpprettSakResponse {

        val httpRequest = HttpRequestBuilder()
        httpRequest.header(HttpHeaders.Authorization, systembrukerService.getAuthorizationHeader())
        httpRequest.header(HttpHeaders.XCorrelationId, correlationId.value)
        httpRequest.method = HttpMethod.Post
        httpRequest.contentType(ContentType.Application.Json)
        httpRequest.body = request
        httpRequest.url(opprettSakUrl)

        return HttpRequest.monitored(
            httpClient = httpClient,
            httpRequest = httpRequest,
            histogram = nySak
        )
    }
}