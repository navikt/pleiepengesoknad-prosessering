package no.nav.helse.dokument

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.response.HttpResponse
import io.ktor.http.*
import io.prometheus.client.Histogram
import no.nav.helse.CorrelationId
import no.nav.helse.HttpRequest
import no.nav.helse.prosessering.v1.Vedlegg
import no.nav.helse.systembruker.SystembrukerService
import java.net.URL

private val lagreDokumentHistogram = Histogram.build(
    "histogram_lagre_dokument",
    "Tidsbruk for lagring av dokument"
).register()

class DokumentGateway(
    private val httpClient: HttpClient,
    private val systembrukerService: SystembrukerService,
    baseUrl : URL
){

    private val completeUrl = HttpRequest.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("v1", "dokument")
    )

    suspend fun lagreDokument(
        dokument: Dokument,
        correlationId: CorrelationId
    ) : URL {

        val httpRequest = HttpRequestBuilder()
        httpRequest.header(HttpHeaders.Authorization, systembrukerService.getAuthorizationHeader())
        httpRequest.header(HttpHeaders.XCorrelationId, correlationId.value)
        httpRequest.header(HttpHeaders.ContentType, ContentType.Application.Json)
        httpRequest.method = HttpMethod.Post
        httpRequest.body = dokument
        httpRequest.url(completeUrl)

        val httpResponse = HttpRequest.monitored<HttpResponse>(
            httpClient = httpClient,
            httpRequest = httpRequest,
            expectedStatusCodes = listOf(HttpStatusCode.Created),
            histogram = lagreDokumentHistogram
        )

        return URL(httpResponse.headers[HttpHeaders.Location])
    }

    data class Dokument(
        val content: ByteArray,
        val contentType: String,
        val title: String
    ) {
        constructor(vedlegg: Vedlegg) : this(content = vedlegg.content, contentType = vedlegg.contentType, title = vedlegg.title)
    }
}