package no.nav.helse.prosessering.v1

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

data class MeldingV1 (
    @JsonAlias("sprak") val språk: String? = null,
    @JsonAlias("soknad_id") val søknadId: String,
    val mottatt: ZonedDateTime,
    @JsonAlias("fra_og_med") val fraOgMed : LocalDate,
    @JsonAlias("til_og_med") val tilOgMed : LocalDate,
    @JsonAlias("soker") val søker : Søker,
    val barn : Barn,
    @JsonAlias("relasjon_til_barnet") val relasjonTilBarnet : String,
    val arbeidsgivere: Arbeidsgivere,
    @JsonAlias("vedlegg_urls") var vedleggUrls : List<URI> = listOf(),
    val medlemskap: Medlemskap,
    @JsonAlias("bekrefter_periode_over_8_uker") val bekrefterPeriodeOver8Uker: Boolean? = null,
    @JsonAlias("utenlandsopphold_i_perioden") val utenlandsoppholdIPerioden: UtenlandsoppholdIPerioden?,
    @JsonAlias("ferieuttak_i_perioden") val ferieuttakIPerioden: FerieuttakIPerioden?,
    @JsonAlias("har_medsoker") val harMedsøker : Boolean,
    @JsonAlias("samtidig_hjemme") val samtidigHjemme: Boolean? = null,
    @JsonAlias("har_forstatt_rettigheter_og_plikter") val harForståttRettigheterOgPlikter : Boolean,
    @JsonAlias("har_bekreftet_opplysninger") val harBekreftetOpplysninger : Boolean,
    val tilsynsordning: Tilsynsordning?,
    val beredskap: Beredskap?,
    @JsonAlias("nattevaak") val nattevåk: Nattevaak?,
    val frilans: Frilans?,
    @JsonAlias("selvstendig_virksomheter") val selvstendigVirksomheter: List<Virksomhet>? = null,
    @JsonAlias("skal_bekrefte_omsorg") val skalBekrefteOmsorg: Boolean? = null, // TODO: Fjern optional når prodsatt.
    @JsonAlias("skal_passe_pa_barnet_i_hele_perioden") val skalPassePaBarnetIHelePerioden: Boolean? = null, // TODO: Fjern optional når prodsatt.
    @JsonAlias("beskrivelse_omsorgsrollen") val beskrivelseOmsorgsrollen: String? = null // TODO: Fjern optional når prodsatt.
)

data class Virksomhet(
    val næringstyper: List<Næringstyper>,
    @JsonAlias("fiskerErPåBladB")
    val fiskerErPåBladB: Boolean? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonAlias("fraOgMed")
    val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonAlias("tilOgMed")
    val tilOgMed: LocalDate? = null,
    val næringsinntekt: Int? = null,
    @JsonAlias("navnPåVirksomheten")
    val navnPåVirksomheten: String,
    val organisasjonsnummer: String? = null,
    @JsonAlias("registrertINorge")
    val registrertINorge: Boolean,
    @JsonAlias("registrertIUtlandet")
    val registrertIUtlandet: Land? = null,
    @JsonAlias("yrkesaktivSisteTreFerdigliknedeÅrene")
    val yrkesaktivSisteTreFerdigliknedeÅrene: YrkesaktivSisteTreFerdigliknedeÅrene? = null,
    @JsonAlias("varigEndring")
    val varigEndring: VarigEndring? = null,
    val regnskapsfører: Regnskapsfører? = null,
    val revisor: Revisor? = null
)

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

data class Revisor(
    val navn: String,
    val telefon: String,
    val kanInnhenteOpplysninger: Boolean
)

data class Regnskapsfører(
    val navn: String,
    val telefon: String
)

data class Søker(
    @JsonAlias("aktoerId") val aktørId: String,
    @JsonAlias("fodselsnummer")val fødselsnummer: String,
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String
) {
    override fun toString(): String {
        return "Soker(aktoerId='${aktørId}Id', fornavn='$fornavn', mellomnavn=$mellomnavn, etternavn='$etternavn')"
    }
}

data class Barn(
    @JsonAlias("fodselsnummer") val fødselsnummer: String?,
    val navn : String?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonAlias("fodselsdato") val fødselsdato: LocalDate?,
    @JsonAlias("aktoerId") val aktørId: String?
) {
    override fun toString(): String {
        return "Barn(navn=$navn, aktoerId=$aktørId, fodselsdato=$fødselsdato)"
    }
}

data class Arbeidsgivere(
    val organisasjoner : List<Organisasjon>
)

data class Organisasjon(
    val organisasjonsnummer: String,
    val navn: String?,
    @JsonAlias("skal_jobbe") val skalJobbe: String,
    @JsonAlias("jobber_normalt_timer") val jobberNormaltTimer: Double,
    @JsonAlias("skal_jobbe_prosent")val skalJobbeProsent: Double,
    @JsonAlias("vet_ikke_ekstrainfo") val vetIkkeEkstrainfo: String? = null
)

data class Medlemskap(
    @JsonAlias("har_bodd_i_utlandet_siste_12_mnd")
    val harBoddIUtlandetSiste12Mnd : Boolean,
    @JsonAlias("utenlandsopphold_siste_12_mnd")
    val utenlandsoppholdSiste12Mnd: List<Bosted> = listOf(),
    @JsonAlias("skal_bo_i_utlandet_neste_12_mnd")
    val skalBoIUtlandetNeste12Mnd : Boolean,
    @JsonAlias("utenlandsopphold_neste_12_mnd")
    val utenlandsoppholdNeste12Mnd: List<Bosted> = listOf()
)

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

data class TilsynsordningVetIkke(
    val svar: String,
    val annet: String? = null
) {
    override fun toString(): String {
        return "TilsynsordningVetIkke(svar='$svar')"
    }
}

data class Tilsynsordning(
    val svar: String,
    val ja: TilsynsordningJa?,
    @JsonAlias("vet_ikke") val vetIkke: TilsynsordningVetIkke?
)

data class Nattevaak(
    @JsonAlias("harNattevaak") val harNattevåk: Boolean,
    val tilleggsinformasjon: String?
) {
    override fun toString(): String {
        return "Nattevåk(harNattevåk=$harNattevåk)"
    }
}

data class Frilans(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val startdato: LocalDate,
    val jobberFortsattSomFrilans: Boolean
)

data class Beredskap(
    @JsonAlias("i_beredskap")
    val beredskap: Boolean,
    val tilleggsinformasjon: String?
) {
    override fun toString(): String {
        return "Beredskap(beredskap=$beredskap)"
    }
}

data class Bosted(
    @JsonFormat(pattern = "yyyy-MM-dd") @JsonAlias("fra_og_med") val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") @JsonAlias("til_og_med") val tilOgMed: LocalDate,
    val landkode: String,
    val landnavn: String
) {
    override fun toString(): String {
        return "Utenlandsopphold(fraOgMed=$fraOgMed, tilOgMed=$tilOgMed, landkode='$landkode', landnavn='$landnavn')"
    }
}

data class Utenlandsopphold(
    @JsonFormat(pattern = "yyyy-MM-dd") @JsonAlias("fra_og_med") val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") @JsonAlias("til_og_med") val tilOgMed: LocalDate,
    val landkode: String,
    val landnavn: String,
    @JsonAlias("er_utenfor_eos") val erUtenforEos: Boolean?,
    @JsonAlias("er_barnet_innlagt") val erBarnetInnlagt: Boolean?,
    @JsonAlias("arsak") val årsak: Årsak?
) {
    override fun toString(): String {
        return "Utenlandsopphold(fraOgMed=$fraOgMed, tilOgMed=$tilOgMed, landkode='$landkode', landnavn='$landnavn', erUtenforEos=$erUtenforEos, erBarnetInnlagt=$erBarnetInnlagt, årsak=$årsak)"
    }
}

enum class Årsak {
    BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING,
    BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD,
    ANNET,
}

data class UtenlandsoppholdIPerioden(
    @JsonAlias("skal_oppholde_seg_i_utlandet_i_perioden") val skalOppholdeSegIUtlandetIPerioden: Boolean? = null,
    val opphold: List<Utenlandsopphold> = listOf()
)

data class FerieuttakIPerioden(
    @JsonAlias("skal_ta_ut_ferie_i_periode") val skalTaUtFerieIPerioden: Boolean,
    val ferieuttak: List<Ferieuttak>
) {
    override fun toString(): String {
        return "FerieuttakIPerioden(skalTaUtFerieIPerioden=$skalTaUtFerieIPerioden, ferieuttak=$ferieuttak)"
    }
}

data class Ferieuttak(
    @JsonFormat(pattern = "yyyy-MM-dd") @JsonAlias("fra_og_med") val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") @JsonAlias("til_og_med") val tilOgMed: LocalDate
) {
    override fun toString(): String {
        return "Ferieuttak(fraOgMed=$fraOgMed, tilOgMed=$tilOgMed)"
    }
}
