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
import no.nav.helse.dusseldorf.ktor.client.*
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * https://app-q1.adeo.no/aktoerregister/swagger-ui.html
 */

class AktoerGateway(
    baseUrl: URI,
    private val accessTokenClient: CachedAccessTokenClient
) {
    private companion object {
        private const val HENTE_AKTOER_ID_OPERATION = "hente-aktoer-id"
        private val logger: Logger = LoggerFactory.getLogger("nav.AktoerGateway")
    }

    private val completeUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("api","v1","identer"),
        queryParameters = mapOf(
            "gjeldende" to listOf("true"),
            "identgruppe" to listOf("AktoerId")
        )
    ).toString()

    private val objectMapper = configuredObjectMapper()

    suspend fun getAktoerId(
        fnr: Fodselsnummer,
        correlationId: CorrelationId
    ) : AktoerId {

        val authorizationHeader = accessTokenClient.getAccessToken(setOf("openid")).asAuthoriationHeader()

        val httpRequest = completeUrl
            .httpGet()
            .header(
                HttpHeaders.Authorization to authorizationHeader,
                HttpHeaders.Accept to "application/json",
                "Nav-Consumer-Id" to "pleiepengesoknad-prosessering",
                "Nav-Personidenter" to fnr.value,
                "Nav-Call-Id" to correlationId.value
            )

        val httpResponse = Retry.retry(
            operation = HENTE_AKTOER_ID_OPERATION,
            factor = 3.0
        ) {
            val (request,_, result) = Operation.monitored(
                app = "pleiepengesoknad-prosessering",
                operation = HENTE_AKTOER_ID_OPERATION,
                resultResolver = { 200 == it.second.statusCode }
            ) { httpRequest.awaitStringResponseResult() }
            result.fold(
                { success -> objectMapper.readValue<Map<String,IdentResponse>>(success)},
                { error ->
                    logger.error(error.toString())
                    throw IllegalStateException("Feil ved henting av Aktør ID mot '${request.url}'")
                }
            )
        }


        if (!httpResponse.containsKey(fnr.value)) {
            throw IllegalStateException("Svar fra '$completeUrl' inneholdt ikke data om det forsespurte fødselsnummeret.")
        }

        val identResponse =  httpResponse.get(key = fnr.value)

        if (identResponse!!.feilmelding!= null) {
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

    private fun configuredObjectMapper() : ObjectMapper {
        val objectMapper = jacksonObjectMapper()
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return objectMapper
    }
}

data class Ident(val ident: String, val identgruppe: String)
data class IdentResponse(val feilmelding : String?, val identer : List<Ident>?)