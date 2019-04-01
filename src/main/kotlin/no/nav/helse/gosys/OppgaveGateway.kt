package no.nav.helse.gosys

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.*
import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.dusseldorf.ktor.client.*
import java.net.URL

class OppgaveGateway(
    baseUrl : URL,
    private val systemCredentialsProvider: SystemCredentialsProvider
) {
    private val completeUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("v1", "oppgave")
    )

    private val monitoredHttpClient = MonitoredHttpClient(
        source = "pleiepengesoknad-prosessering",
        destination = "pleiepenger-oppgave",
        httpClient = HttpClient(Apache) {
            install(JsonFeature) {
                serializer = JacksonSerializer { configureObjectMapper(this) }
            }
            engine {
                customizeClient { setProxyRoutePlanner() }
            }
            install (Logging) {
                sl4jLogger("pleiepenger-oppgave")
            }
        }
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

        return monitoredHttpClient.requestAndReceive(
            httpRequestBuilder = httpRequest,
            expectedHttpResponseCodes = setOf(HttpStatusCode.Created)
        )
    }

    private fun configureObjectMapper(objectMapper: ObjectMapper) : ObjectMapper {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        objectMapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
        return objectMapper
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