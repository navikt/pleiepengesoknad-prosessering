package no.nav.helse.prosessering

data class Metadata(
    val version : Int,
    val correlationId : String,
    val requestId : String,
    val attempt : Int = 1
) {
    internal constructor(metadata: Metadata) : this(
        version = metadata.version,
        correlationId = metadata.correlationId,
        requestId = metadata.requestId,
        attempt = metadata.attempt + 1
    )
}