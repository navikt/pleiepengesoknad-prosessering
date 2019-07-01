package no.nav.helse.prosessering.v1

import java.net.URI

data class PreprossesertMeldingV1(
    val dokumenter: List<List<URI>>,
    val soknadId: String,
    val relasjonTilBarnet : String
)