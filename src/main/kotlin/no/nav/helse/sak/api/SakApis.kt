package no.nav.helse.sak.api

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.helse.sak.v1.MeldingV1
import no.nav.helse.sak.v1.MetadataV1
import no.nav.helse.sak.v1.SakV1Service
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("nav.sakApis")

private const val CORRELATION_ID_HEADER = "Nav-Call-Id"


fun Route.sakApis(
    sakV1Service: SakV1Service
) {

    post("v1/sak") {
        val melding = call.receive<MeldingV1>()
        val metadata = MetadataV1(version = 1, correlationId = call.request.getCorrelationId())
        val saksId = sakV1Service.opprettSak(melding = melding, metaData = metadata)
        call.respond(HttpStatusCode.OK, SakResponse(sakId = saksId.value))
    }
}

private fun ApplicationRequest.getCorrelationId(): String {
    return header(CORRELATION_ID_HEADER) ?: throw ManglerCorrelationId()
}

data class SakResponse(val sakId: String)