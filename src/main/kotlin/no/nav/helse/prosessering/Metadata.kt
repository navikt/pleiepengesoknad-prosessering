package no.nav.helse.prosessering

data class Metadata(
    val version : Int,
    val correlationId : String,
    val requestId : String
)