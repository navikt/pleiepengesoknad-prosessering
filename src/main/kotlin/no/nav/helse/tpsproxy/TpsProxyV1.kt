package no.nav.helse.tpsproxy

import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import no.nav.helse.CorrelationId
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

internal class TpsProxyV1(
    baseUrl: URI,
    private val accessTokenClient: AccessTokenClient,
    private val henteNavnScopes: Set<String> = setOf("openid")
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(TpsProxyV1::class.java)
    }

    private val navnUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("navn")
    ).toString()

    private val cachedAccessTokenClient = CachedAccessTokenClient(this.accessTokenClient)

    internal suspend fun navn(ident: Ident, correlationId: CorrelationId): TpsNavn {
        val authorizationHeader = cachedAccessTokenClient
            .getAccessToken(henteNavnScopes)
            .asAuthoriationHeader()

        val httpRequest = navnUrl
            .httpGet()
            .header(
                HttpHeaders.Authorization to authorizationHeader,
                HttpHeaders.Accept to "application/json",
                NavHeaders.ConsumerId to NavHeaderValues.ConsumerId,
                NavHeaders.PersonIdent to ident.value,
                NavHeaders.CallId to correlationId
            )

        logger.restKall(navnUrl)

        val json = Retry.retry(
            operation = "hente-navn",
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val (request, _, result) = Operation.monitored(
                app = "k9-selvbetjening-oppslag",
                operation = "hente-navn",
                resultResolver = { 200 == it.second.statusCode }
            ) { httpRequest.awaitStringResponseResult() }

            result.fold(
                { success -> JSONObject(success) },
                { error ->
                    logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                    logger.error(error.toString())
                    throw IllegalStateException("Feil ved henting av person.")
                }
            )
        }

        return TpsNavn(
            fornavn = json.getStringOrNull("fornavn") ?: "",
            mellomnavn = json.getStringOrNull("mellomnavn"),
            etternavn = json.getStringOrNull("etternavn") ?: ""
        )
    }
}

private class CoroutineRequestContext(
    internal val correlationId: CorrelationId
) : AbstractCoroutineContextElement(Key) {
    internal companion object Key : CoroutineContext.Key<CoroutineRequestContext>
}

data class Ident(internal val value: String)

internal data class TpsNavn(
    internal val fornavn: String,
    internal val mellomnavn: String?,
    internal val etternavn: String
)

data class ForkortetNavn(private val value: String) {
    internal val fornavn: String
    internal val mellomnavn: String?
    internal val etternavn: String
    internal val fulltNavn: String

    init {
        val splittetNavn = value
            .split(" ")
            .filterNot { it.isBlank() }
        val splittetMellomnavn =
            if (splittetNavn.size > 2) splittetNavn.slice(IntRange(2, splittetNavn.size - 1)) else emptyList()
        fornavn = splittetNavn.fornavn()
        mellomnavn = splittetMellomnavn.mellomnavn()
        etternavn = splittetNavn.etternavn()

        fulltNavn = if (mellomnavn.isNullOrEmpty()) {
            "$fornavn $etternavn"
        } else "$fornavn $mellomnavn $etternavn"
    }

    private fun List<String>.etternavn() = if (isEmpty()) "" else first()
    private fun List<String>.fornavn() = if (size > 1) get(1) else ""
    private fun List<String>.mellomnavn() = if (isEmpty()) null else joinToString(" ")
}

fun Logger.restKall(url: String) = info("Utgående kall til $url")
fun JSONObject.getStringOrNull(key: String) = if (has(key) && !isNull(key)) getString(key) else null

object NavHeaders {
    internal const val CallId = "Nav-Call-id"
    internal const val PersonIdent = "Nav-Personident"
    internal const val ConsumerId = "Nav-Consumer-Id"
}

object NavHeaderValues {
    internal const val ConsumerId = "k9-selvbetjening-oppslag"
}