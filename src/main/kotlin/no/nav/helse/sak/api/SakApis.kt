package no.nav.helse.sak.api

import io.ktor.application.call
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import io.ktor.request.receive
import io.ktor.response.ApplicationResponse
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.helse.sak.v1.MeldingV1
import no.nav.helse.sak.v1.MetadataV1
import no.nav.helse.sak.v1.SakV1Service
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("nav.sakApis")

fun Route.sakApis(
    sakV1Service: SakV1Service
) {

    post("v1/sak") {
        val melding = call.receive<MeldingV1>()
        val metadata = MetadataV1(version = 1, correlationId = call.request.getCorrelationId(), requestId = call.response.getRequestId())
        val saksId = sakV1Service.opprettSak(melding = melding, metaData = metadata)
        call.respond(HttpStatusCode.Created, SakResponse(sakId = saksId.value))
    }
}

private fun ApplicationRequest.getCorrelationId(): String {
    return header(HttpHeaders.XCorrelationId) ?: throw ManglerCorrelationId()
}

private fun ApplicationResponse.getRequestId(): String? {
    return headers[HttpHeaders.XRequestId]
}

data class SakResponse(val sakId: String)