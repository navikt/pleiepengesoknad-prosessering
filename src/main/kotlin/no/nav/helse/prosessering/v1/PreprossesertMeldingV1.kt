package no.nav.helse.prosessering.v1

import no.nav.helse.aktoer.AktoerId
import no.nav.helse.aktoer.NorskIdent
import no.nav.helse.felles.*
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
    val søker: PreprossesertSøker,
    val barn: PreprossesertBarn,
    val arbeidsgivere: Arbeidsgivere,
    val medlemskap: Medlemskap,
    val bekrefterPeriodeOver8Uker: Boolean? = null,
    val utenlandsoppholdIPerioden: UtenlandsoppholdIPerioden,
    val ferieuttakIPerioden: FerieuttakIPerioden?,
    val beredskap: Beredskap?,
    val nattevåk: Nattevåk?,
    val tilsynsordning: Tilsynsordning?,
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
        dokumentUrls: List<List<URI>>,
        sokerAktoerId: AktoerId,
        barnAktoerId: AktoerId?,
        barnetsNavn: String?,
        barnetsNorskeIdent: NorskIdent?,
        barnetsFødselsdato: LocalDate?,
    ) : this(
        språk = melding.språk,
        søknadId = melding.søknadId,
        dokumentUrls = dokumentUrls,
        mottatt = melding.mottatt,
        fraOgMed = melding.fraOgMed,
        tilOgMed = melding.tilOgMed,
        søker = PreprossesertSøker(melding.søker, sokerAktoerId),
        barn = PreprossesertBarn(melding.barn, barnetsNavn, barnetsNorskeIdent, barnAktoerId, barnetsFødselsdato),
        arbeidsgivere = melding.arbeidsgivere,
        medlemskap = melding.medlemskap,
        beredskap = melding.beredskap,
        nattevåk = melding.nattevåk,
        tilsynsordning = melding.tilsynsordning,
        harMedsøker = melding.harMedsøker,
        bekrefterPeriodeOver8Uker = melding.bekrefterPeriodeOver8Uker,
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
