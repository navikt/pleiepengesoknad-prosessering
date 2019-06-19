package no.nav.helse.prosessering.api

import io.ktor.application.ApplicationCall
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
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.v1.ProsesseringV1Service
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("nav.prosesseringApis")

fun Route.prosesseringApis(
    synkronProsesseringV1Service: ProsesseringV1Service,
    asynkronProsesseringV1Service: ProsesseringV1Service
) {

    post("v1/soknad") {
        if (call.request.prosesserAsynkront()) call.handleWith(asynkronProsesseringV1Service)
        else call.handleWith(synkronProsesseringV1Service)
    }
}

private fun ApplicationRequest.prosesserAsynkront() : Boolean {
    val queryParameterValue = queryParameters["async"]
    return queryParameterValue != null && queryParameterValue.equals("true", true)
}

private suspend fun ApplicationCall.handleWith(prosesseringsV1Service: ProsesseringV1Service) {
    val melding = receive<MeldingV1>()
    val metadata = Metadata(
        version = 1,
        correlationId = request.getCorrelationId(),
        requestId = response.getRequestId()
    )
    prosesseringsV1Service.leggSoknadTilProsessering(melding = melding, metadata = metadata)
    respond(HttpStatusCode.Accepted)
}

private fun ApplicationRequest.getCorrelationId(): String {
    return header(HttpHeaders.XCorrelationId) ?: throw IllegalStateException("Correlation Id ikke satt")
}

private fun ApplicationResponse.getRequestId(): String? {
    return headers[HttpHeaders.XRequestId]
}