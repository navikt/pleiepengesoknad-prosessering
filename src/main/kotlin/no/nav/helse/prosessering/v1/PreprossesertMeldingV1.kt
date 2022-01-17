package no.nav.helse.prosessering.v1

import no.nav.helse.felles.*
import no.nav.k9.søknad.Søknad
import java.time.LocalDate
import java.time.ZonedDateTime

data class PreprossesertMeldingV1(
    val språk: String?,
    val søknadId: String,
    val dokumentId: List<List<String>>,
    val mottatt: ZonedDateTime,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val søker: Søker,
    val barn: Barn,
    val medlemskap: Medlemskap,
    val utenlandsoppholdIPerioden: UtenlandsoppholdIPerioden,
    val ferieuttakIPerioden: FerieuttakIPerioden?,
    val beredskap: Beredskap?,
    val nattevåk: Nattevåk?,
    val omsorgstilbud: Omsorgstilbud? = null,
    val harMedsøker: Boolean,
    val frilans: Frilans? = null,
    val selvstendigNæringsdrivende: SelvstendigNæringsdrivende? = null,
    val arbeidsgivere: List<ArbeidsforholdAnsatt>? = null,
    val barnRelasjon: BarnRelasjon? = null,
    val barnRelasjonBeskrivelse: String? = null,
    val samtidigHjemme: Boolean? = null,
    val harForstattRettigheterOgPlikter: Boolean,
    val harBekreftetOpplysninger: Boolean,
    val harVærtEllerErVernepliktig: Boolean? = null,
    val k9FormatSøknad: Søknad
) {
    internal constructor(
        melding: MeldingV1,
        dokumentId: List<List<String>>
    ) : this(
        språk = melding.språk,
        søknadId = melding.søknadId,
        dokumentId = dokumentId,
        mottatt = melding.mottatt,
        fraOgMed = melding.fraOgMed,
        tilOgMed = melding.tilOgMed,
        søker = melding.søker,
        barn = melding.barn,
        medlemskap = melding.medlemskap,
        beredskap = melding.beredskap,
        nattevåk = melding.nattevåk,
        omsorgstilbud = melding.omsorgstilbud,
        harMedsøker = melding.harMedsøker,
        frilans = melding.frilans,
        selvstendigNæringsdrivende = melding.selvstendigNæringsdrivende,
        arbeidsgivere = melding.arbeidsgivere,
        harForstattRettigheterOgPlikter = melding.harForståttRettigheterOgPlikter,
        harBekreftetOpplysninger = melding.harBekreftetOpplysninger,
        utenlandsoppholdIPerioden = melding.utenlandsoppholdIPerioden,
        ferieuttakIPerioden = melding.ferieuttakIPerioden,
        barnRelasjon = melding.barnRelasjon,
        barnRelasjonBeskrivelse = melding.barnRelasjonBeskrivelse,
        samtidigHjemme = melding.samtidigHjemme,
        harVærtEllerErVernepliktig = melding.harVærtEllerErVernepliktig,
        k9FormatSøknad = melding.k9FormatSøknad
    )
}
