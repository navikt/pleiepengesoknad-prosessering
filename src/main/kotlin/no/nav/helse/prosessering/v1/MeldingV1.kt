package no.nav.helse.prosessering.v1

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.helse.felles.*
import no.nav.k9.søknad.Søknad
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

data class MeldingV1 (
    val språk: String? = null,
    val søknadId: String,
    val mottatt: ZonedDateTime,
    val fraOgMed : LocalDate,
    val tilOgMed : LocalDate,
    val søker : Søker,
    val barn : Barn,
    val arbeidsgivere: Arbeidsgivere,
    var vedleggUrls : List<URI> = listOf(),
    val medlemskap: Medlemskap,
    val bekrefterPeriodeOver8Uker: Boolean? = null,
    val utenlandsoppholdIPerioden: UtenlandsoppholdIPerioden,
    val ferieuttakIPerioden: FerieuttakIPerioden?,
    val harMedsøker : Boolean,
    val samtidigHjemme: Boolean? = null,
    val harForståttRettigheterOgPlikter : Boolean,
    val harBekreftetOpplysninger : Boolean,
    val tilsynsordning: Tilsynsordning? = null, // TODO: 10/05/2021 utgår
    val omsorgstilbud: Omsorgstilbud? = null,
    val beredskap: Beredskap?,
    val nattevåk: Nattevåk?,
    val frilans: Frilans?,
    val selvstendigVirksomheter: List<Virksomhet> = listOf(),
    val selvstendigArbeidsforhold: Arbeidsforhold? = null,
    val skalBekrefteOmsorg: Boolean? = null, // TODO: Fjern optional når prodsatt.
    val skalPassePaBarnetIHelePerioden: Boolean? = null, // TODO: Fjern optional når prodsatt.
    val beskrivelseOmsorgsrollen: String? = null, // TODO: Fjern optional når prodsatt.
    val barnRelasjon: BarnRelasjon? = null,
    val barnRelasjonBeskrivelse: String? = null,
    val harVærtEllerErVernepliktig: Boolean,
    val k9FormatSøknad: Søknad
)
