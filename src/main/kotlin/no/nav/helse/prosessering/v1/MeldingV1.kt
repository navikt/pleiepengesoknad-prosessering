package no.nav.helse.prosessering.v1

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonFormat
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
    val tilsynsordning: Tilsynsordning?, // TODO: 10/05/2021 utgår
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
    val harVærtEllerErVernepliktig: Boolean
)

enum class BarnRelasjon(val utskriftsvennlig: String) {
    MOR("Du er mor til barnet"),
    MEDMOR("Du er medmor til barnet"),
    FAR("Du er far til barnet"),
    FOSTERFORELDER("Du er fosterforelder til barnet"),
    ANNET("Annet")
}

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
    val regnskapsfører: Regnskapsfører? = null
) {
    override fun toString(): String {
        return "Virksomhet()"
    }
}

data class Land(val landkode: String, val landnavn: String)

enum class Næringstyper(val beskrivelse: String) {
    FISKE("Fiske"),
    JORDBRUK_SKOGBRUK("Jordbruk/skogbruk"),
    DAGMAMMA("Dagmamma eller familiebarnehage i eget hjem"),
    ANNEN("Annen");
}

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

data class Søker(
    val aktørId: String,
    val fødselsnummer: String,
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String
) {
    override fun toString(): String {
        return "Soker(aktoerId='*****', fornavn='$fornavn', mellomnavn=$mellomnavn, etternavn='$etternavn')"
    }
}

data class Barn(
    val fødselsnummer: String?,
    val navn : String?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fødselsdato: LocalDate?,
    val aktørId: String?
) {
    override fun toString(): String {
        return "Barn(navn=$navn, aktoerId=*****, fodselsdato=$fødselsdato)"
    }
}

data class Arbeidsgivere(
    val organisasjoner : List<Organisasjon>
)

data class Organisasjon(
    val organisasjonsnummer: String,
    val navn: String?,
    val skalJobbe: SkalJobbe,
    val jobberNormaltTimer: Double,
    val skalJobbeProsent: Double,
    val vetIkkeEkstrainfo: String? = null,
    val arbeidsform: Arbeidsform
) {
    override fun toString(): String {
        return "Organisasjon()"
    }
}

enum class Arbeidsform(val utskriftsvennlig: String){
    FAST("Fast antall timer per uke"),
    TURNUS("Turnus"),
    VARIERENDE("Deltid/varierende/tilkalling")
}

data class Medlemskap(
    val harBoddIUtlandetSiste12Mnd : Boolean,
    val utenlandsoppholdSiste12Mnd: List<Bosted> = listOf(),
    val skalBoIUtlandetNeste12Mnd : Boolean,
    val utenlandsoppholdNeste12Mnd: List<Bosted> = listOf()
)

data class Omsorgstilbud(
    val vetOmsorgstilbud: VetOmsorgstilbud,
    val fasteDager: OmsorgstilbudFasteDager? = null,
    val enkeltDager: List<Omsorgsdag>? = null
)

enum class VetOmsorgstilbud {
    VET_ALLE_TIMER,
    VET_NOEN_TIMER,
    VET_IKKE
}

data class Omsorgsdag(
    val dato: LocalDate,
    val tid: Duration
)

data class OmsorgstilbudFasteDager(
    val mandag: Duration? = null,
    val tirsdag: Duration? = null,
    val onsdag: Duration? = null,
    val torsdag: Duration? = null,
    val fredag: Duration? = null
)

// TODO: 10/05/2021 utgår
data class TilsynsordningJa(
    val mandag: Duration?,
    val tirsdag: Duration?,
    val onsdag: Duration?,
    val torsdag: Duration?,
    val fredag: Duration?,
    val tilleggsinformasjon: String? = null
) {
    override fun toString(): String {
        return "TilsynsordningJa(mandag=$mandag, tirsdag=$tirsdag, onsdag=$onsdag, torsdag=$torsdag, fredag=$fredag)"
    }
}

// TODO: 10/05/2021 utgår
data class TilsynsordningVetIkke(
    val svar: String,
    val annet: String? = null
) {
    override fun toString(): String {
        return "TilsynsordningVetIkke(svar='$svar')"
    }
}

// TODO: 10/05/2021 utgår
data class Tilsynsordning(
    val svar: String,
    val ja: TilsynsordningJa?,
    val vetIkke: TilsynsordningVetIkke?
)

data class Nattevåk(
    val harNattevåk: Boolean,
    val tilleggsinformasjon: String?
) {
    override fun toString(): String {
        return "Nattevåk(harNattevåk=$harNattevåk)"
    }
}

data class Frilans(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val startdato: LocalDate,
    val jobberFortsattSomFrilans: Boolean,
    val arbeidsforhold: Arbeidsforhold? = null
)

data class Beredskap(
    val beredskap: Boolean,
    val tilleggsinformasjon: String?
) {
    override fun toString(): String {
        return "Beredskap(beredskap=$beredskap)"
    }
}

data class Bosted(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val tilOgMed: LocalDate,
    val landkode: String,
    val landnavn: String
) {
    override fun toString(): String {
        return "Utenlandsopphold(fraOgMed=$fraOgMed, tilOgMed=$tilOgMed, landkode='$landkode', landnavn='$landnavn')"
    }
}

data class Utenlandsopphold(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val tilOgMed: LocalDate,
    val landkode: String,
    val landnavn: String,
    val erUtenforEøs: Boolean?,
    val erBarnetInnlagt: Boolean?,
    val perioderBarnetErInnlagt: List<Periode> = listOf(),
    val årsak: Årsak?
) {
    override fun toString(): String {
        return "Utenlandsopphold(fraOgMed=$fraOgMed, tilOgMed=$tilOgMed, landkode='$landkode', landnavn='$landnavn', erUtenforEøs=$erUtenforEøs, erBarnetInnlagt=$erBarnetInnlagt, årsak=$årsak)"
    }
}

data class Periode(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate
)

enum class Årsak(val beskrivelse: String) {
    BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING("Barnet innlagt i helseinstitusjon for norsk offentlig regning"),
    BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD("Barnet innlagt i helseinstitusjon dekket etter avtale med et annet land om trygd"),
    ANNET("Innleggelsen dekkes av søker selv"),
}

data class UtenlandsoppholdIPerioden(
    val skalOppholdeSegIUtlandetIPerioden: Boolean,
    val opphold: List<Utenlandsopphold> = listOf()
)

data class FerieuttakIPerioden(
    val skalTaUtFerieIPerioden: Boolean,
    val ferieuttak: List<Ferieuttak>
) {
    override fun toString(): String {
        return "FerieuttakIPerioden(skalTaUtFerieIPerioden=$skalTaUtFerieIPerioden, ferieuttak=$ferieuttak)"
    }
}

data class Ferieuttak(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val tilOgMed: LocalDate
) {
    override fun toString(): String {
        return "Ferieuttak(fraOgMed=$fraOgMed, tilOgMed=$tilOgMed)"
    }
}

data class Arbeidsforhold(
    val skalJobbe: SkalJobbe,
    val arbeidsform: Arbeidsform,
    val jobberNormaltTimer: Double,
    val skalJobbeTimer: Double,
    val skalJobbeProsent: Double
)

enum class SkalJobbe(val verdi: String) {
    @JsonAlias("ja") JA("ja"),
    @JsonAlias("nei") NEI("nei"),
    @JsonAlias("redusert") REDUSERT("redusert"),
    @JsonAlias("vetIkke") VET_IKKE("vetIkke")
}
