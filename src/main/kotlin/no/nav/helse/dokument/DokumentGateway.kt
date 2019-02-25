package no.nav.helse.dokument

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.response.HttpResponse
import io.ktor.http.*
import no.nav.helse.CorrelationId
import no.nav.helse.HttpRequest
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.systembruker.SystembrukerService
import java.net.URL

class DokumentGateway(
    private val httpClient: HttpClient,
    private val systembrukerService: SystembrukerService,
    baseUrl : URL
){

    private val completeUrl = HttpRequest.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("v1", "dokument")
    )
    suspend fun lagrePdf(
        pdf : ByteArray,
        tittel: String,
        aktoerId: AktoerId,
        correlationId: CorrelationId
    ) : URL {

        val urlWithAktoerId = HttpRequest.buildURL(
            baseUrl = completeUrl,
            queryParameters = mapOf(Pair("aktoer_id", aktoerId.id))
        )

        val httpRequest = HttpRequestBuilder()
        httpRequest.header(HttpHeaders.XCorrelationId, correlationId.value)
        httpRequest.header(HttpHeaders.Authorization, systembrukerService.getAuthorizationHeader())
        httpRequest.method = HttpMethod.Post
        httpRequest.body = MultiPartContent.build {
            add("title", tittel)
            add("content", pdf, filename = "oppsummert_soknad.pdf", contentType = ContentType.parse("application/pdf"))
        }
        httpRequest.url(urlWithAktoerId)


        val httpResponse = HttpRequest.monitored<HttpResponse>(
            httpClient = httpClient,
            httpRequest = httpRequest,
            expectedStatusCodes = listOf(HttpStatusCode.Created)
        )

        return URL(httpResponse.headers[HttpHeaders.Location])
    }
}