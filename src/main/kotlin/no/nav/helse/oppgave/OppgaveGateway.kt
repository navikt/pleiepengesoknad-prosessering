package no.nav.helse.oppgave

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import io.ktor.http.*
import no.nav.helse.CorrelationId
import no.nav.helse.HttpError
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.dusseldorf.ktor.client.*
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.helse.joark.JournalPostId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.net.URI

class OppgaveGateway(
    baseUrl : URI,
    private val accessTokenClient : CachedAccessTokenClient
) {
    private companion object {
        private const val OPPRETTE_OPPGAVE_OPERATION = "opprette-oppgave"
        private val logger: Logger = LoggerFactory.getLogger("nav.OppgaveGateway")
    }

    private val completeUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("v1", "oppgave")
    ).toString()

    private val objectMapper = configuredObjectMapper()

    suspend fun lagOppgave(
        sokerAktoerId: AktoerId,
        barnAktoerId: AktoerId?,
        journalPostId: JournalPostId,
        correlationId: CorrelationId
    ) : OppgaveId {

        val authorizationHeader = accessTokenClient.getAccessToken(setOf("openid")).asAuthoriationHeader()

        val oppgaveRequest = OppgaveRequest(
            soker = Person(sokerAktoerId.id),
            barn = Person(barnAktoerId?.id),
            journalPostId = journalPostId.journalPostId
        )

        val body = objectMapper.writeValueAsBytes(oppgaveRequest)
        val contentStream = { ByteArrayInputStream(body) }

        val httpRequest = completeUrl
            .httpPost()
            .body(contentStream)
            .header(
                HttpHeaders.XCorrelationId to correlationId.value,
                HttpHeaders.Authorization to authorizationHeader,
                HttpHeaders.ContentType to "application/json",
                HttpHeaders.Accept to "application/json"
            )

        val (request, response, result) = Operation.monitored(
            app = "pleiepengesoknad-prosessering",
            operation = OPPRETTE_OPPGAVE_OPERATION,
            resultResolver = { 201 == it.second.statusCode }
        ) { httpRequest.awaitStringResponseResult() }

        return result.fold(
            { success -> objectMapper.readValue(success)},
            { error ->
                logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                logger.error(error.toString())
                throw HttpError(response.statusCode,"Feil ved opprettelse av oppgave.")
            }
        )
    }

    private fun configuredObjectMapper() : ObjectMapper {
        val objectMapper = jacksonObjectMapper()
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