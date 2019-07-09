package no.nav.helse.prosessering.v1

import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI
import java.time.LocalDate
import java.time.ZonedDateTime

data class MeldingV1 (
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
    val grad : Int,
    val harMedsoker : Boolean,
    val harForstattRettigheterOgPlikter : Boolean,
    val harBekreftetOpplysninger : Boolean
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
    val alternativId: String?
)

data class Arbeidsgivere(
    val organisasjoner : List<Organisasjon>
)

data class Organisasjon(
    val organisasjonsnummer: String,
    val navn: String?
)

data class Medlemskap(
    @JsonProperty("har_bodd_i_utlandet_siste_12_mnd")
    val harBoddIUtlandetSiste12Mnd : Boolean,
    @JsonProperty("skal_bo_i_utlandet_neste_12_mnd")
    val skalBoIUtlandetNeste12Mnd : Boolean
)

data class Vedlegg (
    val content : ByteArray,
    val contentType : String,
    val title : String
)
