package no.nav.helse.prosessering.v1

import no.nav.helse.felles.Barn
import no.nav.helse.felles.BarnRelasjon
import no.nav.helse.felles.Beredskap
import no.nav.helse.felles.FerieuttakIPerioden
import no.nav.helse.felles.Frilans
import no.nav.helse.felles.Medlemskap
import no.nav.helse.felles.Nattevåk
import no.nav.helse.felles.Omsorgstilbud
import no.nav.helse.felles.OpptjeningIUtlandet
import no.nav.helse.felles.SelvstendigNæringsdrivende
import no.nav.helse.felles.Søker
import no.nav.helse.felles.UtenlandskNæring
import no.nav.helse.felles.UtenlandsoppholdIPerioden
import no.nav.k9.søknad.Søknad
import java.time.LocalDate
import java.time.ZonedDateTime

data class PreprossesertMeldingV1(
    val apiDataVersjon: String? = null,
    val språk: String?,
    val søknadId: String,
    val dokumentId: List<List<String>>,
    val mottatt: ZonedDateTime,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val søker: Søker,
    val barn: Barn,
    val medlemskap: Medlemskap,
    val utenlandsoppholdIPerioden: UtenlandsoppholdIPerioden,
    val opptjeningIUtlandet: List<OpptjeningIUtlandet>,
    val utenlandskNæring: List<UtenlandskNæring>,
    val ferieuttakIPerioden: FerieuttakIPerioden?,
    val beredskap: Beredskap?,
    val nattevåk: Nattevåk?,
    val omsorgstilbud: Omsorgstilbud? = null,
    val frilans: Frilans? = null,
    val selvstendigNæringsdrivende: SelvstendigNæringsdrivende? = null,
    val arbeidsgivere: List<Arbeidsgiver>,
    val barnRelasjon: BarnRelasjon? = null,
    val barnRelasjonBeskrivelse: String? = null,
    val harForstattRettigheterOgPlikter: Boolean,
    val harBekreftetOpplysninger: Boolean,
    val harVærtEllerErVernepliktig: Boolean? = null,
    val k9FormatSøknad: Søknad
) {
    internal constructor(
        melding: MeldingV1,
        dokumentId: List<List<String>>
    ) : this(
        språk = melding.språk,
        søknadId = melding.søknadId,
        dokumentId = dokumentId,
        mottatt = melding.mottatt,
        fraOgMed = melding.fraOgMed,
        tilOgMed = melding.tilOgMed,
        søker = melding.søker,
        barn = melding.barn,
        medlemskap = melding.medlemskap,
        beredskap = melding.beredskap,
        nattevåk = melding.nattevåk,
        omsorgstilbud = melding.omsorgstilbud,
        frilans = melding.frilans,
        selvstendigNæringsdrivende = melding.selvstendigNæringsdrivende,
        opptjeningIUtlandet = melding.opptjeningIUtlandet,
        utenlandskNæring = melding.utenlandskNæring,
        arbeidsgivere = melding.arbeidsgivere,
        harForstattRettigheterOgPlikter = melding.harForståttRettigheterOgPlikter,
        harBekreftetOpplysninger = melding.harBekreftetOpplysninger,
        utenlandsoppholdIPerioden = melding.utenlandsoppholdIPerioden,
        ferieuttakIPerioden = melding.ferieuttakIPerioden,
        barnRelasjon = melding.barnRelasjon,
        barnRelasjonBeskrivelse = melding.barnRelasjonBeskrivelse,
        harVærtEllerErVernepliktig = melding.harVærtEllerErVernepliktig,
        k9FormatSøknad = melding.k9FormatSøknad
    )
}
