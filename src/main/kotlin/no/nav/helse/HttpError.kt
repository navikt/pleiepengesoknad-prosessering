package no.nav.helse

internal class HttpError(httpStatus : Int, message: String) : Throwable("HTTP $httpStatus -> $message")