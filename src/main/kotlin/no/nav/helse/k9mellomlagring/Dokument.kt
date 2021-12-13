package no.nav.helse.k9mellomlagring

import com.fasterxml.jackson.annotation.JsonProperty

data class Dokument(
    val eier: DokumentEier,
    val content: ByteArray,
    @JsonProperty("content_type")
    val contentType: String,
    val title: String
)

data class DokumentEier(
    @JsonProperty("eiers_fødselsnummer")
    val eiersFødselsnummer: String
)