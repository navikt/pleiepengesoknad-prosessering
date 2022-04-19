package no.nav.helse.felles

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class SelvstendigNæringsdrivende(
    val harInntektSomSelvstendig: Boolean,
    val virksomhet: Virksomhet? = null,
    val arbeidsforhold: Arbeidsforhold? = null
)

data class Virksomhet(
    val næringstyper: List<Næringstyper>,
    val fiskerErPåBladB: Boolean? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val tilOgMed: LocalDate? = null,
    val næringsinntekt: Int? = null,
    val navnPåVirksomheten: String,
    val organisasjonsnummer: String? = null,
    val registrertINorge: Boolean,
    val registrertIUtlandet: Land? = null,
    val yrkesaktivSisteTreFerdigliknedeÅrene: YrkesaktivSisteTreFerdigliknedeÅrene? = null,
    val varigEndring: VarigEndring? = null,
    val regnskapsfører: Regnskapsfører? = null,
    val harFlereAktiveVirksomheter: Boolean? = null // TODO: 04/06/2021 - Fjerne optional når api og fronend er prodsatt
) {
    override fun toString(): String {
        return "Virksomhet()"
    }
}

enum class Næringstyper(val beskrivelse: String) {
    FISKE("Fiske"),
    JORDBRUK_SKOGBRUK("Jordbruk/skogbruk"),
    DAGMAMMA("Dagmamma eller familiebarnehage i eget hjem"),
    ANNEN("Annen");
}

data class Land(
    val landkode: String,
    val landnavn: String
)

data class YrkesaktivSisteTreFerdigliknedeÅrene(
    val oppstartsdato: LocalDate
)

data class VarigEndring(
    val dato: LocalDate,
    val inntektEtterEndring: Int,
    val forklaring: String
)

data class Regnskapsfører(
    val navn: String,
    val telefon: String
)