package no.nav.helse.prosessering.v1

import no.nav.helse.felles.Arbeidsforhold
import no.nav.helse.felles.Arbeidsgivere
import no.nav.helse.felles.Barn
import no.nav.helse.felles.BarnRelasjon
import no.nav.helse.felles.Beredskap
import no.nav.helse.felles.FerieuttakIPerioden
import no.nav.helse.felles.Frilans
import no.nav.helse.felles.Medlemskap
import no.nav.helse.felles.Nattevåk
import no.nav.helse.felles.OmsorgstilbudV2
import no.nav.helse.felles.Søker
import no.nav.helse.felles.UtenlandsoppholdIPerioden
import no.nav.helse.felles.Virksomhet
import no.nav.k9.søknad.Søknad
import java.net.URI
import java.time.LocalDate
import java.time.ZonedDateTime

data class PreprossesertMeldingV1(
    val språk: String?,
    val søknadId: String,
    val dokumentUrls: List<List<URI>>,
    val mottatt: ZonedDateTime,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val søker: Søker,
    val barn: Barn,
    val arbeidsgivere: Arbeidsgivere,
    val medlemskap: Medlemskap,
    val utenlandsoppholdIPerioden: UtenlandsoppholdIPerioden,
    val ferieuttakIPerioden: FerieuttakIPerioden?,
    val beredskap: Beredskap?,
    val nattevåk: Nattevåk?,
    val omsorgstilbudV2: OmsorgstilbudV2? = null,
    val harMedsøker: Boolean,
    val frilans: Frilans? = null,
    val selvstendigVirksomheter: List<Virksomhet> = listOf(),
    val selvstendigArbeidsforhold: Arbeidsforhold? = null,
    val barnRelasjon: BarnRelasjon? = null,
    val barnRelasjonBeskrivelse: String? = null,
    val skalBekrefteOmsorg: Boolean? = null, //TODO: Fjerne optinal når prodsatt
    val beskrivelseOmsorgsrollen: String? = null, // TODO: Fjern optional når prodsatt
    val samtidigHjemme: Boolean? = null,
    val harForstattRettigheterOgPlikter: Boolean,
    val harBekreftetOpplysninger: Boolean,
    val harVærtEllerErVernepliktig: Boolean? = null,
    val k9FormatSøknad: Søknad
) {
    internal constructor(
        melding: MeldingV1,
        dokumentUrls: List<List<URI>>
    ) : this(
        språk = melding.språk,
        søknadId = melding.søknadId,
        dokumentUrls = dokumentUrls,
        mottatt = melding.mottatt,
        fraOgMed = melding.fraOgMed,
        tilOgMed = melding.tilOgMed,
        søker = melding.søker,
        barn = melding.barn,
        arbeidsgivere = melding.arbeidsgivere,
        medlemskap = melding.medlemskap,
        beredskap = melding.beredskap,
        nattevåk = melding.nattevåk,
        omsorgstilbudV2 = melding.omsorgstilbudV2,
        harMedsøker = melding.harMedsøker,
        frilans = melding.frilans,
        selvstendigVirksomheter = melding.selvstendigVirksomheter,
        selvstendigArbeidsforhold = melding.selvstendigArbeidsforhold,
        harForstattRettigheterOgPlikter = melding.harForståttRettigheterOgPlikter,
        harBekreftetOpplysninger = melding.harBekreftetOpplysninger,
        utenlandsoppholdIPerioden = melding.utenlandsoppholdIPerioden,
        ferieuttakIPerioden = melding.ferieuttakIPerioden,
        barnRelasjon = melding.barnRelasjon,
        barnRelasjonBeskrivelse = melding.barnRelasjonBeskrivelse,
        skalBekrefteOmsorg = melding.skalBekrefteOmsorg,
        beskrivelseOmsorgsrollen = melding.beskrivelseOmsorgsrollen,
        samtidigHjemme = melding.samtidigHjemme,
        harVærtEllerErVernepliktig = melding.harVærtEllerErVernepliktig,
        k9FormatSøknad = melding.k9FormatSøknad
    )
}
