package no.nav.helse.gosys

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
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
import java.time.ZonedDateTime

class JoarkGateway(
    baseUrl : URL,
    private val systemCredentialsProvider: SystemCredentialsProvider
) {

    private val completeUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("v1","journalforing")
    )

    private val monitoredHttpClient = MonitoredHttpClient(
        source = "pleiepengesoknad-prosessering",
        destination = "pleiepenger-joark",
        httpClient = HttpClient(Apache) {
            install(JsonFeature) {
                serializer = JacksonSerializer { configureObjectMapper(this) }
            }
            engine {
                customizeClient { setProxyRoutePlanner() }
            }
            install (Logging) {
                sl4jLogger("pleiepenger-joark")
            }
        }
    )


    suspend fun journalfoer(
        aktoerId: AktoerId,
        mottatt: ZonedDateTime,
        dokumenter: List<List<URL>>,
        correlationId: CorrelationId
    ) : JournalPostId {

        val request = JoarkRequest(
            aktoerId = aktoerId.id,
            mottatt = mottatt,
            dokumenter = dokumenter
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
        objectMapper.registerModule(JavaTimeModule())
        return objectMapper
    }
}
private data class JoarkRequest(
    val aktoerId: String,
    val mottatt: ZonedDateTime,
    val dokumenter: List<List<URL>>
)

data class JournalPostId(val journalPostId: String)