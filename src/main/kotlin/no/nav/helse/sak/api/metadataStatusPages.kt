package no.nav.helse.sak.api

import io.ktor.application.call
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import no.nav.helse.DefaultError
import java.net.URI

fun StatusPages.Configuration.metadataStatusPages() {

    exception<ManglerCorrelationId> { cause ->
        val error = DefaultError(
            type = URI("/error/missing-correlation-id-header"),
            status = HttpStatusCode.BadRequest.value,
            title = cause.message!!
        )
        call.respond(HttpStatusCode.BadRequest, error)
        throw cause
    }
}