package no.nav.helse.felles

import java.time.LocalDate

class UtenlandskNæring(
    val næringstype: Næringstyper,
    val navnPåVirksomheten: String,
    val land: Land,
    val identifikasjonsnummer: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate? = null
)