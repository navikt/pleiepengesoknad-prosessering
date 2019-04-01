package no.nav.helse.gosys

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.*
import no.nav.helse.CorrelationId
import no.nav.helse.HttpRequest
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.dusseldorf.ktor.client.SystemCredentialsProvider
import no.nav.helse.dusseldorf.ktor.client.buildURL
import java.net.URL

class OppgaveGateway(
    private val httpClient : HttpClient,
    baseUrl : URL,
    private val systemCredentialsProvider: SystemCredentialsProvider
) {
    private val completeUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("v1", "oppgave")
    )

    suspend fun lagOppgave(
        sokerAktoerId: AktoerId,
        barnAktoerId: AktoerId?,
        journalPostId: JournalPostId,
        correlationId: CorrelationId
    ) : OppgaveId {

        val request = OppgaveRequest(
            soker = Person(sokerAktoerId.id),
            barn = Person(barnAktoerId?.id),
            journalPostId = journalPostId.journalPostId
        )

        val httpRequest = HttpRequestBuilder()
        httpRequest.header(HttpHeaders.XCorrelationId, correlationId.value)
        httpRequest.header(HttpHeaders.Authorization, systemCredentialsProvider.getAuthorizationHeader())
        httpRequest.method = HttpMethod.Post
        httpRequest.contentType(ContentType.Application.Json)
        httpRequest.body = request
        httpRequest.url(completeUrl)

        return HttpRequest.monitored(
            httpClient = httpClient,
            expectedStatusCodes = listOf(HttpStatusCode.Created),
            httpRequest = httpRequest
        )
    }
}
private data class OppgaveRequest(
    val soker : Person,
    val barn : Person,
    val journalPostId: String
)
private data class Person(
    val aktoerId: String?
)

data class OppgaveId(val oppgaveId: String)