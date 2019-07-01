package no.nav.helse.dokument

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpDelete
import com.github.kittinunf.fuel.httpPost
import io.ktor.http.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.helse.CorrelationId
import no.nav.helse.HttpError
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.dusseldorf.ktor.client.*
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.helse.prosessering.v1.Vedlegg
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.net.URI
import java.time.Duration

class DokumentGateway(
    private val accessTokenClient: CachedAccessTokenClient,
    baseUrl : URI
){

    private companion object {
        private const val LAGRE_DOKUMENT_OPERATION = "lagre-dokument"
        private const val SLETTE_DOKUMENT_OPERATION = "slette-dokument"
        private val logger: Logger = LoggerFactory.getLogger("nav.DokumentGateway")
    }

    private val completeUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("v1", "dokument")
    )

    private val objectMapper = configuredObjectMapper()

    internal suspend fun lagreDokmenter(
        dokumenter: Set<Dokument>,
        aktoerId: AktoerId,
        correlationId: CorrelationId
    ) : List<URI> {
        val authorizationHeader = accessTokenClient.getAccessToken(setOf("openid")).asAuthoriationHeader()

        return coroutineScope {
            val deferred = mutableListOf<Deferred<URI>>()
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
        urls: List<URI>,
        aktoerId: AktoerId,
        correlationId: CorrelationId
    ) {
        val authorizationHeader = accessTokenClient.getAccessToken(setOf("openid")).asAuthoriationHeader()

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
        url: URI,
        aktoerId: AktoerId,
        correlationId: CorrelationId,
        authorizationHeader: String
    ) {

        val urlMedEier = Url.buildURL(
            baseUrl = url,
            queryParameters = mapOf("eier" to listOf(aktoerId.id))
        ).toString()

        val httpRequest = urlMedEier
            .httpDelete()
            .header(
                HttpHeaders.Authorization to authorizationHeader,
                HttpHeaders.XCorrelationId to correlationId.value
            )

        val (request, _, result) = Operation.monitored(
            app = "pleiepengesoknad-prosessering",
            operation = SLETTE_DOKUMENT_OPERATION,
            resultResolver = { 204 == it.second.statusCode }
        ) {
            httpRequest.awaitStringResponseResult()
        }


        result.fold(
            {},
            { error ->
                logger.warn("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                logger.warn("Feil ved sletting av dokument. $error")
            }
        )
    }

    private suspend fun requestLagreDokument(
        dokument: Dokument,
        aktoerId: AktoerId,
        correlationId: CorrelationId,
        authorizationHeader: String
    ) : URI {

        val urlMedEier = Url.buildURL(
            baseUrl = completeUrl,
            queryParameters = mapOf("eier" to listOf(aktoerId.id))
        ).toString()

        val body = objectMapper.writeValueAsBytes(dokument)
        val contentStream = { ByteArrayInputStream(body) }

        return Retry.retry(
            operation = LAGRE_DOKUMENT_OPERATION,
            initialDelay = Duration.ofMillis(200),
            factor = 2.0
        ) {
            val (request, response, result) = Operation.monitored(
                app = "pleiepengesoknad-prosessering",
                operation = LAGRE_DOKUMENT_OPERATION,
                resultResolver = { 201 == it.second.statusCode }
            ) {
                urlMedEier
                    .httpPost()
                    .body(contentStream)
                    .header(
                        HttpHeaders.Authorization to authorizationHeader,
                        HttpHeaders.XCorrelationId to correlationId.value,
                        HttpHeaders.ContentType to "application/json"
                    )
                    .awaitStringResponseResult()
            }
            result.fold(
                { URI(response.header(HttpHeaders.Location).first()) },
                { error ->
                    logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                    logger.error(error.toString())
                    throw HttpError(response.statusCode, "Feil ved lagring av dokument.")
                }
            )
        }
    }

    private fun configuredObjectMapper() : ObjectMapper {
        val objectMapper = jacksonObjectMapper()
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