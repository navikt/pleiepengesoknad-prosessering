package no.nav.helse.prosessering.v1

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

data class MeldingV1 (
    val sprak: String? = null,
    val soknadId: String,
    val mottatt: ZonedDateTime,
    val fraOgMed : LocalDate,
    val tilOgMed : LocalDate,
    val soker : Soker,
    val barn : Barn,
    val relasjonTilBarnet : String,
    val arbeidsgivere: Arbeidsgivere,
    var vedleggUrls : List<URI> = listOf(),
    val medlemskap: Medlemskap,
    @JsonProperty("utenlandsopphold_i_perioden")
    val utenlandsoppholdIPerioden: UtenlandsoppholdIPerioden?,
    @JsonProperty("ferieuttak_i_perioden")
    val ferieuttakIPerioden: FerieuttakIPerioden?,
    val grad : Int?,
    val harMedsoker : Boolean,
    val samtidigHjemme: Boolean? = null,
    val harForstattRettigheterOgPlikter : Boolean,
    val harBekreftetOpplysninger : Boolean,
    val dagerPerUkeBorteFraJobb: Double? = null,
    val tilsynsordning: Tilsynsordning?,
    val beredskap: Beredskap?,
    val nattevaak: Nattevaak?,
    val frilans: Frilans?
)

data class Soker(
    val aktoerId: String,
    val fodselsnummer: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
) {
    override fun toString(): String {
        return "Soker(aktoerId='$aktoerId', fornavn='$fornavn', mellomnavn=$mellomnavn, etternavn='$etternavn')"
    }
}

data class Barn(
    val fodselsnummer: String?,
    val navn : String?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fodselsdato: LocalDate?,
    val aktoerId: String?
) {
    override fun toString(): String {
        return "Barn(navn=$navn, aktoerId=$aktoerId, fodselsdato=$fodselsdato)"
    }
}

data class Arbeidsgivere(
    val organisasjoner : List<Organisasjon>
)

data class Organisasjon(
    val organisasjonsnummer: String,
    val navn: String?,
    val skalJobbe: String? = null,
    val jobberNormaltTimer: Double? = null,
    val skalJobbeProsent: Double?  = null,
    val vetIkkeEkstrainfo: String? = null
)

data class Medlemskap(
    @JsonProperty("har_bodd_i_utlandet_siste_12_mnd")
    val harBoddIUtlandetSiste12Mnd : Boolean,
    @JsonProperty("utenlandsopphold_siste_12_mnd")
    val utenlandsoppholdSiste12Mnd: List<Bosted> = listOf(),
    @JsonProperty("skal_bo_i_utlandet_neste_12_mnd")
    val skalBoIUtlandetNeste12Mnd : Boolean,
    @JsonProperty("utenlandsopphold_neste_12_mnd")
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
    val vetIkke: TilsynsordningVetIkke?
)

data class Nattevaak(
    val harNattevaak: Boolean,
    val tilleggsinformasjon: String?
) {
    override fun toString(): String {
        return "Nattevaak(harNattevaak=$harNattevaak)"
    }
}

data class Frilans(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val startdato: LocalDate,
    val jobberFortsattSomFrilans: Boolean
)

data class Beredskap(
    @JsonProperty("i_beredskap")
    val beredskap: Boolean,
    val tilleggsinformasjon: String?
) {
    override fun toString(): String {
        return "Beredskap(beredskap=$beredskap)"
    }
}

data class Bosted(
    @JsonFormat(pattern = "yyyy-MM-dd") val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") val tilOgMed: LocalDate,
    val landkode: String,
    val landnavn: String
) {
    override fun toString(): String {
        return "Utenlandsopphold(fraOgMed=$fraOgMed, tilOgMed=$tilOgMed, landkode='$landkode', landnavn='$landnavn')"
    }
}

data class Utenlandsopphold(
    @JsonFormat(pattern = "yyyy-MM-dd") val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") val tilOgMed: LocalDate,
    val landkode: String,
    val landnavn: String,
    val erUtenforEos: Boolean?,
    val erBarnetInnlagt: Boolean?,
    val arsak: Arsak?
) {
    override fun toString(): String {
        return "Utenlandsopphold(fraOgMed=$fraOgMed, tilOgMed=$tilOgMed, landkode='$landkode', landnavn='$landnavn', erUtenforEos=$erUtenforEos, erBarnetInnlagt=$erBarnetInnlagt, arsak=$arsak)"
    }
}

enum class Arsak {
    BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING,
    BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD,
    ANNET,
}

data class UtenlandsoppholdIPerioden(
    @JsonProperty("skal_oppholde_seg_i_utlandet_i_perioden")
    val skalOppholdeSegIUtlandetIPerioden: Boolean? = null,
    val opphold: List<Utenlandsopphold> = listOf()
)

data class FerieuttakIPerioden(
    @JsonProperty("skal_ta_ut_ferie_i_periode")
    val skalTaUtFerieIPerioden: Boolean,
    val ferieuttak: List<Ferieuttak>
) {
    override fun toString(): String {
        return "FerieuttakIPerioden(skalTaUtFerieIPerioden=$skalTaUtFerieIPerioden, ferieuttak=$ferieuttak)"
    }
}

data class Ferieuttak(
    @JsonFormat(pattern = "yyyy-MM-dd") val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") val tilOgMed: LocalDate
) {
    override fun toString(): String {
        return "Ferieuttak(fraOgMed=$fraOgMed, tilOgMed=$tilOgMed)"
    }
}