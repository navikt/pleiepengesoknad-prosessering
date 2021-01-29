package no.nav.helse.prosessering.v2

import no.nav.helse.felles.*
import java.net.URI
import java.time.LocalDate
import java.time.ZonedDateTime

data class MeldingV2 (
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
    val tilsynsordning: Tilsynsordning?,
    val beredskap: Beredskap?,
    val nattevåk: Nattevåk?,
    val frilans: Frilans?,
    val selvstendigVirksomheter: List<Virksomhet> = listOf(),
    val skalBekrefteOmsorg: Boolean? = null, // TODO: Fjern optional når prodsatt.
    val skalPassePaBarnetIHelePerioden: Boolean? = null, // TODO: Fjern optional når prodsatt.
    val beskrivelseOmsorgsrollen: String? = null, // TODO: Fjern optional når prodsatt.
    val barnRelasjon: BarnRelasjon? = null,
    val barnRelasjonBeskrivelse: String? = null
)
