package no.nav.helse.prosessering.v1

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.aktoer.NorskIdent
import java.net.URI
import java.time.LocalDate
import java.time.ZonedDateTime

data class PreprossesertMeldingV1(
    @JsonAlias("sprak") val språk: String?,
    @JsonAlias("soknad_id") val søknadId: String,
    @JsonAlias("dokument_urls") val dokumentUrls: List<List<URI>>,
    val mottatt: ZonedDateTime,
    @JsonAlias("fra_og_med") val fraOgMed: LocalDate,
    @JsonAlias("til_og_med") val tilOgMed: LocalDate,
    @JsonAlias("soker") val søker: PreprossesertSøker,
    val barn: PreprossesertBarn,
    @JsonAlias("relasjon_til_barnet") val relasjonTilBarnet: String,
    val arbeidsgivere: Arbeidsgivere,
    val medlemskap: Medlemskap,
    @JsonAlias("bekrefter_periode_over_8_uker") val bekrefterPeriodeOver8Uker: Boolean? = null,
    @JsonAlias("utenlandsopphold_i_perioden") val utenlandsoppholdIPerioden: UtenlandsoppholdIPerioden?,
    @JsonAlias("ferieuttak_i_perioden") val ferieuttakIPerioden: FerieuttakIPerioden?,
    val beredskap: Beredskap?,
    @JsonAlias("nattevaak") val nattevåk: Nattevåk?,
    val tilsynsordning: Tilsynsordning?,
    @JsonAlias("har_medsoker") val harMedsøker: Boolean,
    val frilans: Frilans? = null,
    @JsonAlias("selvstendig_virksomheter") val selvstendigVirksomheter: List<Virksomhet>? = null,
    @JsonAlias("har_forstatt_rettigheter_og_plikter") val harForstattRettigheterOgPlikter: Boolean,
    @JsonAlias("har_bekreftet_opplysninger") val harBekreftetOpplysninger: Boolean
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
        søker = PreprossesertSøker(melding.søker, sokerAktoerId),
        barn = PreprossesertBarn(melding.barn, barnetsNavn, barnetsNorskeIdent, barnAktoerId, barnetsFødselsdato),
        relasjonTilBarnet = melding.relasjonTilBarnet,
        arbeidsgivere = melding.arbeidsgivere,
        medlemskap = melding.medlemskap,
        beredskap = melding.beredskap,
        nattevåk = melding.nattevåk,
        tilsynsordning = melding.tilsynsordning,
        harMedsøker = melding.harMedsøker,
        bekrefterPeriodeOver8Uker = melding.bekrefterPeriodeOver8Uker,
        frilans = melding.frilans,
        selvstendigVirksomheter = melding.selvstendigVirksomheter,
        harForstattRettigheterOgPlikter = melding.harForståttRettigheterOgPlikter,
        harBekreftetOpplysninger = melding.harBekreftetOpplysninger,
        utenlandsoppholdIPerioden = melding.utenlandsoppholdIPerioden,
        ferieuttakIPerioden = melding.ferieuttakIPerioden
    )
}

data class PreprossesertSøker(
    @JsonAlias("fodselsnummer") val fødselsnummer: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    @JsonAlias("aktoer_id") val aktørId: String
) {
    internal constructor(soker: Søker, aktoerId: AktoerId) : this(
        fødselsnummer = soker.fødselsnummer,
        fornavn = soker.fornavn,
        mellomnavn = soker.mellomnavn,
        etternavn = soker.etternavn,
        aktørId = aktoerId.id
    )
}

data class PreprossesertBarn(
    @JsonAlias("fodselsnummer") val fødselsnummer: String?,
    val navn: String?,
    @JsonAlias("fodselsdato") val fødselsdato: LocalDate?,
    @JsonAlias("aktoer_id") val aktørId: String?
) {

    internal constructor(
        barn: Barn,
        barnetsNavn: String?,
        barnetsNorskeIdent: NorskIdent?,
        aktoerId: AktoerId?,
        fødselsdato: LocalDate?
    ) : this(
        fødselsnummer = barn.fødselsnummer ?: barnetsNorskeIdent?.getValue(),
        navn = barnetsNavn,
        fødselsdato = fødselsdato,
        aktørId = aktoerId?.id
    )

    override fun toString(): String {
        return "PreprossesertBarn(navn=$navn, aktoerId=$aktørId, fodselsdato=$fødselsdato)"
    }
}
