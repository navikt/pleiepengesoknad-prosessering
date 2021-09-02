package no.nav.helse.felles

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Duration
import java.time.LocalDate

enum class BarnRelasjon(val utskriftsvennlig: String) {
    MOR("Du er mor til barnet"),
    MEDMOR("Du er medmor til barnet"),
    FAR("Du er far til barnet"),
    FOSTERFORELDER("Du er fosterforelder til barnet"),
    ANNET("Annet")
}


data class Land(val landkode: String, val landnavn: String)

data class Søker(
    val aktørId: String,
    val fødselsnummer: String,
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String
) {
    override fun toString(): String {
        return "Soker()"
    }
}

data class Barn(
    val fødselsnummer: String,
    val navn : String
) {
    override fun toString(): String {
        return "Barn()"
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
    val arbeidsform: Arbeidsform? = null //TODO 09.02.2021 - Fjerner optional når prodsatt
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
    val fasteDager: OmsorgstilbudUkedager? = null
)

data class Omsorgsdag(
    val dato: LocalDate,
    val tid: Duration
)

enum class VetOmsorgstilbud {
    VET_ALLE_TIMER,
    VET_IKKE
}

data class OmsorgstilbudUkedager(
    val mandag: Duration? = null,
    val tirsdag: Duration? = null,
    val onsdag: Duration? = null,
    val torsdag: Duration? = null,
    val fredag: Duration? = null
)

data class OmsorgstilbudV2(
    val historisk: HistoriskOmsorgstilbud? = null,
    val planlagt: PlanlagtOmsorgstilbud? = null
)

data class HistoriskOmsorgstilbud(
    val enkeltdager: List<Omsorgsdag>
)

data class PlanlagtOmsorgstilbud(
    val enkeltdager: List<Omsorgsdag>? = null,
    val ukedager: OmsorgstilbudUkedager? = null,
    val vetOmsorgstilbud: VetOmsorgstilbud,
    val erLiktHverDag: Boolean? = null
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
    @JsonFormat(pattern = "yyyy-MM-dd")
    val sluttdato: LocalDate? = null,
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

fun Søker.tilTpsNavn(): Navn = Navn(
    fornavn = fornavn,
    mellomnavn = mellomnavn,
    etternavn = etternavn
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
)

data class Arbeidsforhold(
    val skalJobbe: SkalJobbe,
    val arbeidsform: Arbeidsform,
    val jobberNormaltTimer: Double,
    val skalJobbeProsent: Double
)

enum class SkalJobbe(val verdi: String) {
    @JsonAlias("ja") JA("ja"), // TODO: 28/05/2021 Fjern @JsonAlias etter prodsetting.
    @JsonAlias("nei") NEI("nei"), // TODO: 28/05/2021 Fjern @JsonAlias etter prodsetting.
    @JsonAlias("redusert") REDUSERT("redusert"), // TODO: 28/05/2021 Fjern @JsonAlias etter prodsetting.
    @JsonAlias("vetIkke") VET_IKKE("vetIkke") // TODO: 28/05/2021 Fjern @JsonAlias etter prodsetting.
}
