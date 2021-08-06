package no.nav.helse.prosessering

import com.fasterxml.jackson.annotation.JsonProperty

data class Metadata(
    val version : Int,
    @JsonProperty("correlationId")
    val correlationId : String
)
