package no.nav.helse.prosessering.v1

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
    val grad : Int?,
    val harMedsoker : Boolean,
    val samtidigHjemme: Boolean? = null,
    val harForstattRettigheterOgPlikter : Boolean,
    val harBekreftetOpplysninger : Boolean,
    val dagerPerUkeBorteFraJobb: Double? = null,
    val tilsynsordning: Tilsynsordning?,
    val beredskap: Beredskap?,
    val nattevaak: Nattevaak?
)

data class Soker(
    val aktoerId: String,
    val fodselsnummer: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
)
data class Barn(
    val fodselsnummer: String?,
    val navn : String?,
    val alternativId: String?,
    val aktoerId: String?
)

data class Arbeidsgivere(
    val organisasjoner : List<Organisasjon>
)

data class Organisasjon(
    val organisasjonsnummer: String,
    val navn: String?,
    val jobberNormaltTimer: Double? = null,
    val skalJobbeProsent: Double?  = null,
    val vetIkkeEkstrainfo: String? = null
)

data class Medlemskap(
    @JsonProperty("har_bodd_i_utlandet_siste_12_mnd")
    val harBoddIUtlandetSiste12Mnd : Boolean,
    @JsonProperty("skal_bo_i_utlandet_neste_12_mnd")
    val skalBoIUtlandetNeste12Mnd : Boolean
)

data class TilsynsordningJa(
    val mandag: Duration?,
    val tirsdag: Duration?,
    val onsdag: Duration?,
    val torsdag: Duration?,
    val fredag: Duration?,
    val tilleggsinformasjon: String? = null
)

data class TilsynsordningVetIkke(
    val svar: String,
    val annet: String? = null
)

data class Tilsynsordning(
    val svar: String,
    val ja: TilsynsordningJa?,
    val vetIkke: TilsynsordningVetIkke?
)

data class Nattevaak(
    val harNattevaak: Boolean,
    val tilleggsinformasjon: String?
)

data class Beredskap(
    @JsonProperty("i_beredskap")
    val beredskap: Boolean,
    val tilleggsinformasjon: String?
)