package no.nav.helse

import io.ktor.application.call
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond

fun StatusPages.Configuration.defaultStatusPages() {

    exception<Throwable> { cause ->
        val error = DefaultError(
            status = HttpStatusCode.InternalServerError.value,
            title = "Uhåndtert feil."
        )
        call.respond(HttpStatusCode.InternalServerError, error)
        throw cause
    }
}