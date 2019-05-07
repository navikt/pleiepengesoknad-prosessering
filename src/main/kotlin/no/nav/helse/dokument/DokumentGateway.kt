package no.nav.helse.dokument

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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.dusseldorf.ktor.client.*
import no.nav.helse.prosessering.v1.Vedlegg
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL


class DokumentGateway(
    private val systemCredentialsProvider: SystemCredentialsProvider,
    baseUrl : URL
){

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger("nav.DokumentGateway")
    }

    private val completeUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("v1", "dokument")
    )

    private val monitoredHttpClient = MonitoredHttpClient(
        source = "pleiepengesoknad-prosessering",
        destination = "pleiepenger-dokument",
        httpClient = HttpClient(Apache) {
            install(JsonFeature) {
                serializer = JacksonSerializer { configureObjectMapper(this) }
            }
            engine {
                customizeClient { setProxyRoutePlanner() }
            }
            install (Logging) {
                sl4jLogger("pleiepenger-dokument")
            }
        }
    )

    internal suspend fun lagreDokmenter(
        dokumenter: Set<Dokument>,
        aktoerId: AktoerId,
        correlationId: CorrelationId
    ) : List<URL> {
        val authorizationHeader = systemCredentialsProvider.getAuthorizationHeader()

        return coroutineScope {
            val deferred = mutableListOf<Deferred<URL>>()
            dokumenter.forEach {
                deferred.add(async {
                    requestLagreDokument(
                        dokument = it,
                        correlationId = correlationId,
                        aktoerId = aktoerId,
                        authorizationHeader = authorizationHeader
                    )
                })
            }
            deferred.awaitAll()
        }
    }

    internal suspend fun slettDokmenter(
        urls: List<URL>,
        aktoerId: AktoerId,
        correlationId: CorrelationId
    ) {
        val authorizationHeader = systemCredentialsProvider.getAuthorizationHeader()

        coroutineScope {
            val deferred = mutableListOf<Deferred<Unit>>()
            urls.forEach {
                deferred.add(async {
                    requestSlettDokument(
                        url = it,
                        correlationId = correlationId,
                        aktoerId = aktoerId,
                        authorizationHeader = authorizationHeader
                    )
                })
            }
            deferred.awaitAll()
        }
    }

    private suspend fun requestSlettDokument(
        url: URL,
        aktoerId: AktoerId,
        correlationId: CorrelationId,
        authorizationHeader: String
    ) {

        val urlMedEier = Url.buildURL(
            baseUrl = url,
            queryParameters = mapOf("eier" to listOf(aktoerId.id))
        )

        val httpRequest = HttpRequestBuilder()
        httpRequest.header(HttpHeaders.Authorization, authorizationHeader)
        httpRequest.header(HttpHeaders.XCorrelationId, correlationId.value)
        httpRequest.method = HttpMethod.Delete
        httpRequest.url(urlMedEier)

        try {
            monitoredHttpClient.request(
                httpRequestBuilder = httpRequest,
                expectedHttpResponseCodes = setOf(HttpStatusCode.NoContent)
            ).use {}
        } catch (cause: Throwable) {
            logger.warn("Feil ved sletting av dokument '$url' for aktør '${aktoerId.id}'", cause)
        }
    }

    private suspend fun requestLagreDokument(
        dokument: Dokument,
        aktoerId: AktoerId,
        correlationId: CorrelationId,
        authorizationHeader: String
    ) : URL {

        val urlMedEier = Url.buildURL(
            baseUrl = completeUrl,
            queryParameters = mapOf("eier" to listOf(aktoerId.id))
        )
        val httpRequest = HttpRequestBuilder()
        httpRequest.header(HttpHeaders.Authorization, authorizationHeader)
        httpRequest.header(HttpHeaders.XCorrelationId, correlationId.value)
        httpRequest.header(HttpHeaders.ContentType, ContentType.Application.Json)
        httpRequest.method = HttpMethod.Post
        httpRequest.body = dokument
        httpRequest.url(urlMedEier)

        val httpResponse = monitoredHttpClient.request(
            httpRequestBuilder = httpRequest,
            expectedHttpResponseCodes = setOf(HttpStatusCode.Created)
        )

        return httpResponse.use {
            URL(it.headers[HttpHeaders.Location])
        }
    }

    private fun configureObjectMapper(objectMapper: ObjectMapper) : ObjectMapper {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        objectMapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
        return objectMapper
    }

    data class Dokument(
        val content: ByteArray,
        val contentType: String,
        val title: String
    ) {
        constructor(vedlegg: Vedlegg) : this(content = vedlegg.content, contentType = vedlegg.contentType, title = vedlegg.title)
    }
}