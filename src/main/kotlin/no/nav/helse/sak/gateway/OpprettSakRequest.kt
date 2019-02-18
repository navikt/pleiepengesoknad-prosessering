package no.nav.helse.sak.gateway

data class OpprettSakRequest(
    val tema: String,
    val applikasjon: String,
    val aktoerId: String,
    val orgnr: String? = null, // Kun integrasjon mot en person per nå, ikke organisasjonsnummer i denne contexten.
    val fagsakNr: String? = null // Vi har ingen fagsak enda, opprettelse av sak er det første steget vi gjør.
)