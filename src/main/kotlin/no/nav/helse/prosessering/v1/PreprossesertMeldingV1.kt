package no.nav.helse.prosessering.v1

import no.nav.helse.aktoer.AktoerId
import no.nav.helse.prosessering.SoknadId
import java.net.URI
import java.time.LocalDate
import java.time.ZonedDateTime

data class PreprossesertMeldingV1(
    val id: String, // Unik ID for å teknisk kunne merge topics med informasjon som gjelder samme søknad
    val dokumentUrls: List<List<URI>>, // Alle dokumenter i søknaden, genererte og vedlegg.

    // Tilsvarende MeldingV1
    val mottatt: ZonedDateTime,
    val fraOgMed : LocalDate,
    val tilOgMed : LocalDate,
    val soker : PreprossesertSoker,
    val barn : PreprossesertBarn,
    val relasjonTilBarnet : String,
    val arbeidsgivere: Arbeidsgivere,
    val medlemskap: Medlemskap,
    val grad : Int,
    val harMedsoker : Boolean,
    val harForstattRettigheterOgPlikter : Boolean,
    val harBekreftetOpplysninger : Boolean
) {
    internal constructor(
        soknadId: SoknadId,
        melding: MeldingV1,
        dokumentUrls: List<List<URI>>,
        sokerAktoerId: AktoerId,
        barnAktoerId: AktoerId?
    ) : this(
        id = soknadId.id,
        dokumentUrls = dokumentUrls,
        mottatt = melding.mottatt,
        fraOgMed = melding.fraOgMed,
        tilOgMed = melding.tilOgMed,
        soker = PreprossesertSoker(melding.soker, sokerAktoerId),
        barn = PreprossesertBarn(melding.barn, barnAktoerId),
        relasjonTilBarnet = melding.relasjonTilBarnet,
        arbeidsgivere = melding.arbeidsgivere,
        medlemskap = melding.medlemskap,
        grad = melding.grad,
        harMedsoker = melding.harMedsoker,
        harForstattRettigheterOgPlikter = melding.harForstattRettigheterOgPlikter,
        harBekreftetOpplysninger = melding.harBekreftetOpplysninger
    )
}

data class PreprossesertSoker(
    val fodselsnummer: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val aktoerId: String
) {
    internal constructor(soker: Soker, aktoerId: AktoerId) : this(
        fodselsnummer = soker.fodselsnummer,
        fornavn = soker.fornavn,
        mellomnavn = soker.mellomnavn,
        etternavn = soker.etternavn,
        aktoerId = aktoerId.id
    )
}

data class PreprossesertBarn(
    val fodselsnummer: String?,
    val navn : String?,
    val alternativId: String?,
    val aktoerId: String?
) {
    internal constructor(barn: Barn, aktoerId: AktoerId?) : this(
        fodselsnummer = barn.fodselsnummer,
        navn = barn.navn,
        alternativId = barn.alternativId,
        aktoerId = aktoerId?.id
    )
}