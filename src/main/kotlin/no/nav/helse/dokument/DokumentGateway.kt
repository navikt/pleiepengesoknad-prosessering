package no.nav.helse.dokument

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
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
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.net.URI
import java.time.Duration

class DokumentGateway(
    private val accessTokenClient: AccessTokenClient,
    private val lagreDokumentScopes: Set<String>,
    private val sletteDokumentScopes: Set<String>,
    baseUrl : URI
) : HealthCheck {

    private companion object {
        private const val LAGRE_DOKUMENT_OPERATION = "lagre-dokument"
        private const val SLETTE_DOKUMENT_OPERATION = "slette-dokument"
        private val logger: Logger = LoggerFactory.getLogger(DokumentGateway::class.java)
    }

    private val completeUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("v1", "dokument")
    )

    private val objectMapper = configuredObjectMapper()
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    override suspend fun check(): Result {
        val checkGetLagreDokumentAccessToken = checkGetAccessToken(LAGRE_DOKUMENT_OPERATION, lagreDokumentScopes)
        val checkGetSletteDokumentAccessToken = checkGetAccessToken(SLETTE_DOKUMENT_OPERATION, sletteDokumentScopes)
        val combined = checkGetLagreDokumentAccessToken.result().toMutableMap()
        combined.putAll(checkGetSletteDokumentAccessToken.result())
        combined["name"] = "DokumentGateway"
        return if (checkGetLagreDokumentAccessToken is UnHealthy || checkGetSletteDokumentAccessToken is UnHealthy) UnHealthy(combined)
        else Healthy(combined)
    }

    private fun checkGetAccessToken(
        operation: String,
        scopes: Set<String>
    ) : Result {
        return try {
            accessTokenClient.getAccessToken(scopes)
            Healthy(mapOf(operation to "Henting av access token OK"))
        } catch (cause: Throwable) {
            logger.error("Feil ved henting av access token for henting av dokument", cause)
            UnHealthy(mapOf(operation to "Henting av access token feilet"))
        }
    }


    internal suspend fun lagreDokmenter(
        dokumenter: Set<Dokument>,
        aktørId: String,
        correlationId: CorrelationId
    ) : List<URI> {
        val authorizationHeader = cachedAccessTokenClient.getAccessToken(lagreDokumentScopes).asAuthoriationHeader()

        return coroutineScope {
            val deferred = mutableListOf<Deferred<URI>>()
            dokumenter.forEach {
                deferred.add(async {
                    requestLagreDokument(
                        dokument = it,
                        correlationId = correlationId,
                        aktoerId = aktørId,
                        authorizationHeader = authorizationHeader
                    )
                })
            }
            deferred.awaitAll()
        }
    }

    internal suspend fun slettDokmenter(
        urls: List<URI>,
        aktoerId: String,
        correlationId: CorrelationId
    ) {
        val authorizationHeader = cachedAccessTokenClient.getAccessToken(sletteDokumentScopes).asAuthoriationHeader()

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
        aktoerId: String,
        correlationId: CorrelationId,
        authorizationHeader: String
    ) {

        val urlMedEier = Url.buildURL(
            baseUrl = url,
            queryParameters = mapOf("eier" to listOf(aktoerId))
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
        aktoerId: String,
        correlationId: CorrelationId,
        authorizationHeader: String
    ) : URI {

        val urlMedEier = Url.buildURL(
            baseUrl = completeUrl,
            queryParameters = mapOf("eier" to listOf(aktoerId))
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
        objectMapper.propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        return objectMapper
    }

    data class Dokument(
        val content: ByteArray,
        val contentType: String,
        val title: String
    )
}
