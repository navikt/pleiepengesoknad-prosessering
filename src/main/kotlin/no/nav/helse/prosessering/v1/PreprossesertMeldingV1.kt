package no.nav.helse.prosessering.v1

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.aktoer.NorskIdent
import java.net.URI
import java.time.LocalDate
import java.time.ZonedDateTime

data class PreprossesertMeldingV1(
    val sprak: String?,
    val soknadId: String,
    val dokumentUrls: List<List<URI>>,
    val mottatt: ZonedDateTime,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val soker: PreprossesertSoker,
    val barn: PreprossesertBarn,
    val relasjonTilBarnet: String,
    val arbeidsgivere: Arbeidsgivere,
    val medlemskap: Medlemskap,
    @JsonProperty("utenlandsopphold_i_perioden")
    val utenlandsoppholdIPerioden: UtenlandsoppholdIPerioden?,
    @JsonProperty("ferieuttak_i_perioden")
    val ferieuttakIPerioden: FerieuttakIPerioden?,
    val grad: Int?,
    val beredskap: Beredskap?,
    val nattevaak: Nattevaak?,
    val tilsynsordning: Tilsynsordning?,
    val harMedsoker: Boolean,
    val frilans: Frilans? = null,
    val selvstendigVirksomheter: List<Virksomhet>? = null,
    val harForstattRettigheterOgPlikter: Boolean,
    val harBekreftetOpplysninger: Boolean
) {
    internal constructor(
        melding: MeldingV1,
        dokumentUrls: List<List<URI>>,
        sokerAktoerId: AktoerId,
        barnAktoerId: AktoerId?,
        barnetsNavn: String?,
        barnetsNorskeIdent: NorskIdent?,
        barnetsFødselsdato: LocalDate?
    ) : this(
        sprak = melding.sprak,
        soknadId = melding.soknadId,
        dokumentUrls = dokumentUrls,
        mottatt = melding.mottatt,
        fraOgMed = melding.fraOgMed,
        tilOgMed = melding.tilOgMed,
        soker = PreprossesertSoker(melding.soker, sokerAktoerId),
        barn = PreprossesertBarn(melding.barn, barnetsNavn, barnetsNorskeIdent, barnAktoerId, barnetsFødselsdato),
        relasjonTilBarnet = melding.relasjonTilBarnet,
        arbeidsgivere = melding.arbeidsgivere,
        medlemskap = melding.medlemskap,
        grad = melding.grad,
        beredskap = melding.beredskap,
        nattevaak = melding.nattevaak,
        tilsynsordning = melding.tilsynsordning,
        harMedsoker = melding.harMedsoker,
        frilans = melding.frilans,
        selvstendigVirksomheter = melding.selvstendigVirksomheter,
        harForstattRettigheterOgPlikter = melding.harForstattRettigheterOgPlikter,
        harBekreftetOpplysninger = melding.harBekreftetOpplysninger,
        utenlandsoppholdIPerioden = melding.utenlandsoppholdIPerioden,
        ferieuttakIPerioden = melding.ferieuttakIPerioden
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
    val navn: String?,
    val fodselsdato: LocalDate?,
    val aktoerId: String?
) {

    internal constructor(
        barn: Barn,
        barnetsNavn: String?,
        barnetsNorskeIdent: NorskIdent?,
        aktoerId: AktoerId?,
        fødselsdato: LocalDate?
    ) : this(
        fodselsnummer = barn.fodselsnummer ?: barnetsNorskeIdent?.getValue(),
        navn = barnetsNavn,
        fodselsdato = fødselsdato,
        aktoerId = aktoerId?.id
    )

    override fun toString(): String {
        return "PreprossesertBarn(navn=$navn, aktoerId=$aktoerId, fodselsdato=$fodselsdato)"
    }
}