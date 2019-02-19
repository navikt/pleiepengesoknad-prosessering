package no.nav.helse.prosessering.v1

data class MetadataV1(
    val version : Int,
    val correlationId : String,
    val requestId : String?
)