package no.nav.helse.prosessering.v1

import no.nav.helse.felles.Arbeidsforhold
import no.nav.helse.felles.Barn
import no.nav.helse.felles.BarnRelasjon
import no.nav.helse.felles.Beredskap
import no.nav.helse.felles.FerieuttakIPerioden
import no.nav.helse.felles.Frilans
import no.nav.helse.felles.Medlemskap
import no.nav.helse.felles.Nattevåk
import no.nav.helse.felles.Omsorgstilbud
import no.nav.helse.felles.OpptjeningIUtlandet
import no.nav.helse.felles.SelvstendigNæringsdrivende
import no.nav.helse.felles.Søker
import no.nav.helse.felles.UtenlandskNæring
import no.nav.helse.felles.UtenlandsoppholdIPerioden
import no.nav.k9.søknad.Søknad
import java.time.LocalDate
import java.time.ZonedDateTime

data class MeldingV1 (
    val apiDataVersjon: String? = null,
    val språk: String? = null,
    val søknadId: String,
    val mottatt: ZonedDateTime,
    val fraOgMed : LocalDate,
    val tilOgMed : LocalDate,
    val søker : Søker,
    val barn : Barn,
    var vedleggId : List<String> = listOf(),
    val fødselsattestVedleggId: List<String>? = listOf(), // TODO: Fjern nullabel etter lansering.
    val medlemskap: Medlemskap,
    val utenlandsoppholdIPerioden: UtenlandsoppholdIPerioden,
    val ferieuttakIPerioden: FerieuttakIPerioden?,
    val opptjeningIUtlandet: List<OpptjeningIUtlandet>,
    val utenlandskNæring: List<UtenlandskNæring>,
    val harMedsøker : Boolean,
    val samtidigHjemme: Boolean? = null,
    val harForståttRettigheterOgPlikter : Boolean,
    val harBekreftetOpplysninger : Boolean,
    val omsorgstilbud: Omsorgstilbud? = null,
    val beredskap: Beredskap?,
    val nattevåk: Nattevåk?,
    val frilans: Frilans,
    val selvstendigNæringsdrivende: SelvstendigNæringsdrivende,
    val arbeidsgivere: List<Arbeidsgiver>,
    val barnRelasjon: BarnRelasjon? = null,
    val barnRelasjonBeskrivelse: String? = null,
    val harVærtEllerErVernepliktig: Boolean? = null,
    val k9FormatSøknad: Søknad
) {
    override fun toString(): String {
        return "MeldingV1(apiDataVersjon=$apiDataVersjon, språk=$språk, søknadId='$søknadId', mottatt=$mottatt, fraOgMed=$fraOgMed, tilOgMed=$tilOgMed, vedleggId=$vedleggId, fødselsattestVedleggId=$fødselsattestVedleggId, medlemskap=$medlemskap, utenlandsoppholdIPerioden=$utenlandsoppholdIPerioden, ferieuttakIPerioden=$ferieuttakIPerioden, opptjeningIUtlandet=$opptjeningIUtlandet, utenlandskNæring=$utenlandskNæring, harMedsøker=$harMedsøker, samtidigHjemme=$samtidigHjemme, harForståttRettigheterOgPlikter=$harForståttRettigheterOgPlikter, harBekreftetOpplysninger=$harBekreftetOpplysninger, omsorgstilbud=$omsorgstilbud, beredskap=$beredskap, nattevåk=$nattevåk, frilans=$frilans, selvstendigNæringsdrivende=$selvstendigNæringsdrivende, arbeidsgivere=$arbeidsgivere, barnRelasjon=$barnRelasjon, barnRelasjonBeskrivelse=$barnRelasjonBeskrivelse, harVærtEllerErVernepliktig=$harVærtEllerErVernepliktig)"
    }
}

data class Arbeidsgiver(
    val navn: String? = null,
    val organisasjonsnummer: String,
    val erAnsatt: Boolean,
    val arbeidsforhold: Arbeidsforhold? = null,
    val sluttetFørSøknadsperiode: Boolean? = null
)
