package no.nav.helse.journalforing.gateway

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
import no.nav.helse.systembruker.SystembrukerService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL

private val logger: Logger = LoggerFactory.getLogger("nav.JournalforingGateway")

private val nyJournalforing = Histogram.build(
    "histogram_ny_journalforing_joark",
    "Tidsbruk for ny journalføring mot Joark"
).register()

/*
    https://dokmotinngaaende-q1.nais.preprod.local/rest/mottaInngaaendeForsendelse
 */
class JournalforingGateway(
    private val httpClient: HttpClient,
    private val joarkInngaaendeForsendelseUrl: URL,
    private val systembrukerService: SystembrukerService
) {

    internal suspend fun jorunalfor(request: JournalPostRequest) : JournalPostResponse {

        val httpRequest = HttpRequestBuilder()
        httpRequest.header(HttpHeaders.Authorization, systembrukerService.getAuthorizationHeader())
        httpRequest.method = HttpMethod.Post
        httpRequest.contentType(ContentType.Application.Json)
        httpRequest.body = request
        httpRequest.url(joarkInngaaendeForsendelseUrl)

        return HttpRequest.monitored(
            httpClient = httpClient,
            httpRequest = httpRequest,
            histogram = nyJournalforing
        )
    }
}