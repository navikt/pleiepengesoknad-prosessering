package no.nav.helse.validering

import java.net.URI

data class ValideringsError (
    val type: URI = URI.create("about:blank"),
    val title: String,
    val status: Int,
    val detail: String? = null,
    val instance: URI = URI.create("about:blank"),
    val invalidParameters: List<Brudd> = emptyList()
)