package no.nav.helse.aktoer

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import no.nav.helse.CorrelationId
import no.nav.helse.HttpError
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.core.erGyldigFodselsnummer
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration

/**
 * https://app-q1.adeo.no/aktoerregister/swagger-ui.html
 */

class AktoerGateway(
    baseUrl: URI,
    private val accessTokenClient: AccessTokenClient,
    private val henteAktoerIdScopes: Set<String> = setOf("openid")
) : HealthCheck {

    private companion object {
        private const val HENTE_AKTOER_ID_OPERATION = "hente-aktoer-id"
        private val logger: Logger = LoggerFactory.getLogger(AktoerGateway::class.java)
        private val objectMapper = jacksonObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }

    private val completeUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("api", "v1", "identer"),
        queryParameters = mapOf(
            "gjeldende" to listOf("true"),
            "identgruppe" to listOf("AktoerId")
        )
    ).toString()

    private val fodselsnummerUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("api", "v1", "identer"),
        queryParameters = mapOf(
            Pair("gjeldende", listOf("true")),
            Pair("identgruppe", listOf("NorskIdent"))
        )
    ).toString()

    private val objectMapper = configuredObjectMapper()
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    override suspend fun check(): Result {
        return try {
            accessTokenClient.getAccessToken(henteAktoerIdScopes)
            Healthy("AktoerGateway", "Henting av access token for henting av AktørID OK.")
        } catch (cause: Throwable) {
            logger.error("Feil ved henting av access token for henting av AktørID", cause)
            UnHealthy("AktoerGateway", "Henting av access token for henting av AktørID feilet.")
        }
    }

    suspend fun getAktoerId(
        ident: NorskIdent,
        correlationId: CorrelationId
    ): AktoerId {

        val authorizationHeader = cachedAccessTokenClient.getAccessToken(henteAktoerIdScopes).asAuthoriationHeader()

        val httpRequest = completeUrl
            .httpGet()
            .header(
                HttpHeaders.Authorization to authorizationHeader,
                HttpHeaders.Accept to "application/json",
                "Nav-Consumer-Id" to "pleiepengesoknad-prosessering",
                "Nav-Personidenter" to ident.getValue(),
                "Nav-Call-Id" to correlationId.value
            )

        val httpResponse = Retry.retry(
            operation = HENTE_AKTOER_ID_OPERATION,
            initialDelay = Duration.ofMillis(200),
            factor = 2.0
        ) {
            val (request, response, result) = Operation.monitored(
                app = "pleiepengesoknad-prosessering",
                operation = HENTE_AKTOER_ID_OPERATION,
                resultResolver = { 200 == it.second.statusCode }
            ) { httpRequest.awaitStringResponseResult() }
            result.fold(
                { success -> objectMapper.readValue<Map<String, IdentResponse>>(success) },
                { error ->
                    logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                    logger.error(error.toString())
                    throw HttpError(response.statusCode, "Feil ved henting av Aktør ID.")
                }
            )
        }


        if (!httpResponse.containsKey(ident.getValue())) {
            throw IllegalStateException("Svar fra '$completeUrl' inneholdt ikke data om det forsespurte fødselsnummeret.")
        }

        val identResponse = httpResponse.get(key = ident.getValue())

        if (identResponse!!.feilmelding != null) {
            logger.warn("Mottok feilmelding fra AktørRegister : '${identResponse.feilmelding}'")
        }

        if (identResponse.identer == null) {
            throw IllegalStateException("Fikk 0 AktørID'er for det forsespurte fødselsnummeret mot '$completeUrl'")
        }

        if (identResponse.identer.size != 1) {
            throw IllegalStateException("Fikk ${identResponse.identer.size} AktørID'er for det forsespurte fødselsnummeret mot '$completeUrl'")
        }

        val aktoerId = AktoerId(identResponse.identer[0].ident)
        logger.trace("Resolved AktørID $aktoerId")
        return aktoerId
    }

    private suspend fun get(
        url: String,
        personIdent: String,
        correlationId: CorrelationId
    ): String {
        val authorizationHeader = cachedAccessTokenClient.getAccessToken(henteAktoerIdScopes).asAuthoriationHeader()

        val httpRequest = url
            .httpGet()
            .header(
                HttpHeaders.Authorization to authorizationHeader,
                HttpHeaders.Accept to "application/json",
                "Nav-Consumer-Id" to "pleiepengesoknad-api",
                "Nav-Personidenter" to personIdent,
                "Nav-Call-Id" to correlationId
            )

        val httpResponse = Retry.retry(
            operation = HENTE_AKTOER_ID_OPERATION,
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val (request, _, result) = Operation.monitored(
                app = "pleiepengesoknad-api",
                operation = HENTE_AKTOER_ID_OPERATION,
                resultResolver = { 200 == it.second.statusCode }
            ) { httpRequest.awaitStringResponseResult() }
            result.fold(
                { success -> Companion.objectMapper.readValue<Map<String, AktoerRegisterIdentResponse>>(success) },
                { error ->
                    logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                    logger.error(error.toString())
                    throw IllegalStateException("Feil ved henting av Aktør ID.")
                }
            )
        }


        if (!httpResponse.containsKey(personIdent)) {
            throw IllegalStateException("Svar fra '$url' inneholdt ikke data om det forsespurte fødselsnummeret.")
        }

        val identResponse = httpResponse.get(key = personIdent)

        if (identResponse!!.feilmelding != null) {
            logger.warn("Mottok feilmelding fra AktørRegister : '${identResponse.feilmelding}'")
        }

        if (identResponse.identer == null) {
            throw IllegalStateException("Fikk 0 AktørID'er for det forsespurte fødselsnummeret mot '$url'")
        }

        if (identResponse.identer.size != 1) {
            throw IllegalStateException("Fikk ${identResponse.identer.size} AktørID'er for det forsespurte fødselsnummeret mot '$url'")
        }

        val aktoerId = AktoerId(identResponse.identer[0].ident)
        logger.trace("Resolved AktørID $aktoerId")
        return aktoerId.id
    }

    suspend fun hentNorskIdent(
        aktoerId: AktoerId,
        correlationId: CorrelationId
    ): NorskIdent {
        return get(
            url = fodselsnummerUrl,
            personIdent = aktoerId.id,
            correlationId = correlationId
        ).tilNorskIdent()
    }

    private fun configuredObjectMapper(): ObjectMapper {
        val objectMapper = jacksonObjectMapper()
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return objectMapper
    }
}

fun String.tilNorskIdent(): NorskIdent = if (erGyldigFodselsnummer()) Fodselsnummer(this) else Dnummer(this)

interface NorskIdent {
    fun getValue(): String
}


data class Ident(val ident: String, val identgruppe: String)
data class IdentResponse(val feilmelding: String?, val identer: List<Ident>?)
data class AktoerRegisterIdentResponse(val feilmelding: String?, val identer: List<Ident>?)