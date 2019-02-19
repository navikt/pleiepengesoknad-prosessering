package no.nav.helse.prosessering.v1

import java.net.URL
import java.time.LocalDate
import java.time.ZonedDateTime

data class MeldingV1 (
    val mottatt: ZonedDateTime,
    val fraOgMed : LocalDate,
    val tilOgMed : LocalDate,
    val soker : Soker,
    val barn : Barn,
    val relasjonTilBarnet : String,
    val arbeidsgivere: Arbeidsgivere,
    val vedlegg : List<URL>,
    val medlemskap: Medlemskap
)

data class Soker(
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
    val navn: String
)

data class Medlemskap(
    val harBoddIUtlandetSiste12Mnd : Boolean,
    val skalBoIUtlandetNeste12Mnd : Boolean
)