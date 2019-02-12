package no.nav.helse.sak.gateway

data class OpprettSakRequest(
    val tema: String,
    val applikasjon: String,
    val aktoerId: String
)