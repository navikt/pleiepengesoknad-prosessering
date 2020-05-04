package no.nav.helse.prosessering.v1

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.aktoer.NorskIdent
import java.net.URI
import java.time.LocalDate
import java.time.ZonedDateTime

data class PreprossesertMeldingV1(
    val språk: String?,
    val søknadId: String,
    val dokumentUrls: List<List<URI>>,
    val mottatt: ZonedDateTime,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val soker: PreprossesertSoker,
    val barn: PreprossesertBarn,
    val relasjonTilBarnet: String,
    val arbeidsgivere: Arbeidsgivere,
    val medlemskap: Medlemskap,
    val bekrefterPeriodeOver8Uker: Boolean? = null,
    val utenlandsoppholdIPerioden: UtenlandsoppholdIPerioden?,
    val ferieuttakIPerioden: FerieuttakIPerioden?,
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
        språk = melding.språk,
        søknadId = melding.søknadId,
        dokumentUrls = dokumentUrls,
        mottatt = melding.mottatt,
        fraOgMed = melding.fraOgMed,
        tilOgMed = melding.tilOgMed,
        soker = PreprossesertSoker(melding.søker, sokerAktoerId),
        barn = PreprossesertBarn(melding.barn, barnetsNavn, barnetsNorskeIdent, barnAktoerId, barnetsFødselsdato),
        relasjonTilBarnet = melding.relasjonTilBarnet,
        arbeidsgivere = melding.arbeidsgivere,
        medlemskap = melding.medlemskap,
        beredskap = melding.beredskap,
        nattevaak = melding.nattevåk,
        tilsynsordning = melding.tilsynsordning,
        harMedsoker = melding.harMedsøker,
        bekrefterPeriodeOver8Uker = melding.bekrefterPeriodeOver8Uker,
        frilans = melding.frilans,
        selvstendigVirksomheter = melding.selvstendigVirksomheter,
        harForstattRettigheterOgPlikter = melding.harForståttRettigheterOgPlikter,
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
    internal constructor(soker: Søker, aktoerId: AktoerId) : this(
        fodselsnummer = soker.fødselsnummer,
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
        fodselsnummer = barn.fødselsnummer ?: barnetsNorskeIdent?.getValue(),
        navn = barnetsNavn,
        fodselsdato = fødselsdato,
        aktoerId = aktoerId?.id
    )

    override fun toString(): String {
        return "PreprossesertBarn(navn=$navn, aktoerId=$aktoerId, fodselsdato=$fodselsdato)"
    }
}
