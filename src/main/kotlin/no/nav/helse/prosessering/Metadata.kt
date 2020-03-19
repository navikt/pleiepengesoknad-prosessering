package no.nav.helse.prosessering

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

data class Metadata(
    val version : Int,
    @JsonProperty("correlationId")
    @JsonAlias(value = ["correlation_id"])
    val correlationId : String,
    @JsonProperty("requestId")
    @JsonAlias(value = ["request_id"])
    val requestId : String
)