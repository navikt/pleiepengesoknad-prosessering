package no.nav.helse.sak.api

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("nav.sakApis")

private const val CORRELATION_ID_HEADER = "Nav-Call-Id"


fun Route.sakApis() {

    post("/v1/sak") {
        call.respond(HttpStatusCode.OK, SakResponse(sakId = "1234"))
    }
}

private fun ApplicationRequest.getCorrelationId(): String {
    return header(CORRELATION_ID_HEADER) ?: throw ManglerCorrelationId()
}

data class SakResponse(val sakId: String)