package no.nav.helse.aktoer

import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.helse.tpsproxy.*
import no.nav.helse.tpsproxy.Ident
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import kotlin.coroutines.coroutineContext

internal class AktoerregisterV1(
    baseUrl: URI,
    private val accessTokenClient: AccessTokenClient,
    private val henteAktoerIdScopes : Set<String> = setOf("openid")
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(AktoerregisterV1::class.java)
    }

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    private val aktoerIdUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("identer"),
        queryParameters = mapOf(
            Pair("gjeldende", listOf("true")),
            Pair("identgruppe", listOf("AktoerId"))
        )
    ).toString()

    internal suspend fun aktørId(ident: Ident) : AktørId {
        val authorizationHeader = cachedAccessTokenClient
            .getAccessToken(henteAktoerIdScopes)
            .asAuthoriationHeader()

        val httpRequest = aktoerIdUrl
            .httpGet()
            .header(
                HttpHeaders.Authorization to authorizationHeader,
                HttpHeaders.Accept to "application/json",
                NavHeaders.ConsumerId to NavHeaderValues.ConsumerId,
                NavHeaders.PersonIdenter to ident.value,
                NavHeaders.CallId to coroutineContext.correlationId().value
            )

        logger.restKall(aktoerIdUrl)

        val json = Retry.retry(
            operation = "hente-aktoer-id",
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val (request,_, result) = Operation.monitored(
                app = "k9-selvbetjening-oppslag",
                operation = "hente-aktoer-id",
                resultResolver = { 200 == it.second.statusCode }
            ) { httpRequest.awaitStringResponseResult() }

            result.fold(
                { success -> JSONObject(success)},
                { error ->
                    logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                    logger.error(error.toString())
                    throw IllegalStateException("Feil ved henting av Aktør ID.")
                }
            )
        }

        logger.logResponse(json)

        check(json.has(ident.value)) { "Response inneholdt ikke etterspurt ident. Response = '$json'" }
        val identResponse = json.getJSONObject(ident.value)

        if (json.has("feilmelding")) {
            logger.warn("Mottok feilmelding. Response = '$json'")
        }

        check(identResponse.has("identer")) { "Response inneholdt ikke en liste med identer. Response = '$json'"}
        val identer = identResponse.getJSONArray("identer")

        check(identer.length() == 1) { "Listen med identer inneholder ${identer.length()} entries. Forventet 1. Response = '$json'" }

        return AktørId(identer.getJSONObject(0).getString("ident"))
    }
}

data class AktørId(internal val value: String)