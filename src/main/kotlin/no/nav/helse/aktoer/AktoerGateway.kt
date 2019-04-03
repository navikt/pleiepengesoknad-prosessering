package no.nav.helse.aktoer

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import no.nav.helse.CorrelationId
import no.nav.helse.dusseldorf.ktor.client.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL

/**
 * https://app-q1.adeo.no/aktoerregister/swagger-ui.html
 */

private val logger: Logger = LoggerFactory.getLogger("nav.AktoerGateway")

class AktoerGateway(
    baseUrl: URL,
    private val systemCredentialsProvider: SystemCredentialsProvider
) {
    private val completeUrl : URL = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("api","v1","identer"),
        queryParameters = mapOf(
            "gjeldende" to listOf("true"),
            "identgruppe" to listOf("AktoerId")
        )
    )

    private val monitoredHttpClient = MonitoredHttpClient(
        source = "pleiepengesoknad-prosessering",
        destination = "aktoer-register",
        httpClient = HttpClient(Apache) {
            install(JsonFeature) {
                serializer = JacksonSerializer { configureObjectMapper(this) }
            }
            engine {
                customizeClient { setProxyRoutePlanner() }
            }
            install (Logging) {
                sl4jLogger("aktoer-register")
            }
        }
    )

    suspend fun getAktoerId(
        fnr: Fodselsnummer,
        correlationId: CorrelationId
    ) : AktoerId {

        val httpRequest = HttpRequestBuilder()
        httpRequest.header(HttpHeaders.Authorization, systemCredentialsProvider.getAuthorizationHeader())
        httpRequest.header("Nav-Consumer-Id", "pleiepengesoknad-prosessering")
        httpRequest.header("Nav-Personidenter", fnr.value)
        httpRequest.header("Nav-Call-Id", correlationId.value)
        httpRequest.method = HttpMethod.Get
        httpRequest.accept(ContentType.Application.Json)
        httpRequest.url(completeUrl)


        val httpResponse : Map<String, IdentResponse> = monitoredHttpClient.requestAndReceive(
            httpRequestBuilder = httpRequest
        )

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
    private fun configureObjectMapper(objectMapper: ObjectMapper) : ObjectMapper {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return objectMapper
    }
}

data class Ident(val ident: String, val identgruppe: String)
data class IdentResponse(val feilmelding : String?, val identer : List<Ident>?)