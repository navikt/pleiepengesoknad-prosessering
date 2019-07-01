package no.nav.helse

import io.ktor.http.HttpStatusCode

internal class HttpError(private val httpStatus : Int, message: String) : Throwable("HTTP $httpStatus -> $message") {
    internal fun httpStatusCode() = if (httpStatus < 0) null else HttpStatusCode.fromValue(httpStatus)
}