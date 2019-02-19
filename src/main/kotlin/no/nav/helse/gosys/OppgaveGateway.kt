package no.nav.helse.gosys

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.*
import no.nav.helse.CorrelationId
import no.nav.helse.HttpRequest
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.systembruker.SystembrukerService
import java.net.URL

class OppgaveGateway(
    private val httpClient : HttpClient,
    private val url : URL,
    private val systembrukerService: SystembrukerService
) {
    suspend fun lagOppgave(
        sokerAktoerId: AktoerId,
        barnAktoerId: AktoerId?,
        journalPostId: JournalPostId,
        sakId: SakId,
        correlationId: CorrelationId
    ) : OppgaveId {

        val request = OppgaveRequest(
            soker = Person(sokerAktoerId.id),
            barn = Person(barnAktoerId?.id),
            journalPostId = journalPostId.journalPostId,
            sakId = sakId.sakId
        )

        val httpRequest = HttpRequestBuilder()
        httpRequest.header(HttpHeaders.XCorrelationId, correlationId.value)
        httpRequest.header(HttpHeaders.Authorization, systembrukerService.getAuthorizationHeader())
        httpRequest.method = HttpMethod.Post
        httpRequest.contentType(ContentType.Application.Json)
        httpRequest.body = request
        httpRequest.url(url)

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
    val sakId: String,
    val journalPostId: String
)
private data class Person(
    val aktoerId: String?
)

data class OppgaveId(val oppgaveId: String)