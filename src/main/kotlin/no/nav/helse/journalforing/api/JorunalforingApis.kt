package no.nav.helse.journalforing.api

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.helse.journalforing.v1.JournalforingV1Service
import no.nav.helse.journalforing.v1.MeldingV1
import no.nav.helse.journalforing.v1.MetadataV1
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("nav.journalforingApis")

private const val CORRELATION_ID_HEADER = "Nav-Call-Id"


fun Route.journalforingApis(
    journalforingV1Service: JournalforingV1Service
) {

    post("/v1/journalforing") {
        val melding = call.receive<MeldingV1>()
        val metadata = MetadataV1(version = 1, correlationId = call.request.getCorrelationId())
        val journalPostId = journalforingV1Service.journalfor(melding = melding, metaData = metadata)

        call.respond(HttpStatusCode.OK, Response(journalPostId = journalPostId.value))
    }
}

private fun ApplicationRequest.getCorrelationId(): String {
    return header(CORRELATION_ID_HEADER) ?: throw ManglerCorrelationId()
}

data class Response(val journalPostId: String)