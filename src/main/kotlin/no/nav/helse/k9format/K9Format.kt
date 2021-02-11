@file:Suppress("SpellCheckingInspection")

package no.nav.helse.k9format

import no.nav.helse.felles.*
import no.nav.helse.felles.Beredskap
import no.nav.helse.felles.Nattevåk
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.PreprossesertMeldingV1
import no.nav.helse.prosessering.v1.snittTilsynsTimerPerDag
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.LovbestemtFerie
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.aktivitet.Frilanser
import no.nav.k9.søknad.felles.personopplysninger.Bosteder
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold.UtenlandsoppholdPeriodeInfo
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING
import no.nav.k9.søknad.felles.type.Landkode
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.felles.type.SøknadId
import no.nav.k9.søknad.ytelse.psb.v1.*
import no.nav.k9.søknad.ytelse.psb.v1.Beredskap.BeredskapPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.Nattevåk.NattevåkPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.TilsynPeriodeInfo
import java.time.Duration
import no.nav.k9.søknad.felles.personopplysninger.Barn as K9Barn
import no.nav.k9.søknad.felles.personopplysninger.Søker as K9Søker
import no.nav.k9.søknad.ytelse.psb.v1.Beredskap as K9Beredskap

const val DAGER_PER_UKE = 5

private val k9FormatVersjon = Versjon.of("1.0.0")

fun MeldingV1.tilK9PleiepengesøknadSyktBarn(): Søknad {
    val søknadsPeriode = Periode(fraOgMed, tilOgMed)
    val søknad = Søknad(
        SøknadId.of(søknadId),
        k9FormatVersjon,
        mottatt,
        søker.tilK9Søker(),
        PleiepengerSyktBarn(
            søknadsPeriode,
            byggK9DataBruktTilUtledning(),
            barn.tilK9Barn(),
            byggK9ArbeidAktivitet(),
            beredskap?.tilK9Beredskap(søknadsPeriode),
            nattevåk?.tilK9Nattevåk(søknadsPeriode),
            tilsynsordning?.tilK9Tilsynsordning(søknadsPeriode),
            byggK9Arbeidstid(),
            byggK9Uttak(søknadsPeriode),
            byggK9Omsorg(),
            ferieuttakIPerioden?.tilK9LovbestemtFerie(),
            medlemskap.tilK9Bosteder(),
            utenlandsoppholdIPerioden.tilK9Utenlandsopphold(søknadsPeriode)
        )
    )
    return søknad
}

fun PreprossesertMeldingV1.tilK9PleiepengesøknadSyktBarn(): Søknad {
    val søknadsPeriode = Periode(fraOgMed, tilOgMed)
    val søknad = Søknad(
        SøknadId.of(søknadId),
        k9FormatVersjon,
        mottatt,
        søker.tilK9Søker(),
        PleiepengerSyktBarn(
            søknadsPeriode,
            byggK9DataBruktTilUtledning(),
            barn.tilK9Barn(),
            byggK9ArbeidAktivitet(),
            beredskap?.tilK9Beredskap(søknadsPeriode),
            nattevåk?.tilK9Nattevåk(søknadsPeriode),
            tilsynsordning?.tilK9Tilsynsordning(søknadsPeriode),
            byggK9Arbeidstid(),
            byggK9Uttak(søknadsPeriode),
            byggK9Omsorg(),
            ferieuttakIPerioden?.tilK9LovbestemtFerie(),
            medlemskap.tilK9Bosteder(),
            utenlandsoppholdIPerioden.tilK9Utenlandsopphold(søknadsPeriode)
        )
    )
    return søknad
}

fun MeldingV1.byggK9Omsorg(): Omsorg = Omsorg(
    barnRelasjon?.utskriftsvennlig ?: "Forelder",
    skalBekrefteOmsorg,
    beskrivelseOmsorgsrollen
)

fun PreprossesertMeldingV1.byggK9Omsorg(): Omsorg = Omsorg(
    barnRelasjon?.utskriftsvennlig ?: "Forelder",
    skalBekrefteOmsorg,
    beskrivelseOmsorgsrollen
)

fun Barn.tilK9Barn(): K9Barn = K9Barn(NorskIdentitetsnummer.of(this.fødselsnummer), (this.fødselsdato))

fun PreprossesertBarn.tilK9Barn(): K9Barn =
    K9Barn(NorskIdentitetsnummer.of(this.fødselsnummer), (this.fødselsdato))

fun Søker.tilK9Søker(): K9Søker = K9Søker(NorskIdentitetsnummer.of(fødselsnummer))

fun PreprossesertSøker.tilK9Søker(): K9Søker = K9Søker(NorskIdentitetsnummer.of(fødselsnummer))

fun Frilans.tilK9Frilanser(): Frilanser = Frilanser(startdato, jobberFortsattSomFrilans)

fun Beredskap.tilK9Beredskap(periode: Periode): K9Beredskap? =
    if (!beredskap) null else K9Beredskap(mapOf(periode to BeredskapPeriodeInfo(this.tilleggsinformasjon)))


fun Nattevåk.tilK9Nattevåk(periode: Periode): no.nav.k9.søknad.ytelse.psb.v1.Nattevåk? {
    if (!harNattevåk) return null

    val perioder = mutableMapOf<Periode, NattevåkPeriodeInfo>()

    perioder[periode] = NattevåkPeriodeInfo(tilleggsinformasjon)

    return no.nav.k9.søknad.ytelse.psb.v1.Nattevåk(perioder)
}

fun Tilsynsordning.tilK9Tilsynsordning(
    periode: Periode
): no.nav.k9.søknad.ytelse.psb.v1.tilsyn.Tilsynsordning? = when (svar) {
    "ja" -> no.nav.k9.søknad.ytelse.psb.v1.tilsyn.Tilsynsordning(mutableMapOf(periode to TilsynPeriodeInfo(ja!!.snittTilsynsTimerPerDag())))
    else -> null
}

fun FerieuttakIPerioden.tilK9LovbestemtFerie(): LovbestemtFerie? {
    if (!skalTaUtFerieIPerioden) return null

    val perioder = mutableListOf<Periode>()

    ferieuttak.forEach { ferieuttak ->
        perioder.add(Periode(ferieuttak.fraOgMed, ferieuttak.tilOgMed))
    }

    return LovbestemtFerie(perioder)
}

fun Medlemskap.tilK9Bosteder(): Bosteder? {
    if (!harBoddIUtlandetSiste12Mnd && !skalBoIUtlandetNeste12Mnd) return null
    val perioder = mutableMapOf<Periode, Bosteder.BostedPeriodeInfo>()

    utenlandsoppholdSiste12Mnd.forEach { bosted ->
        perioder[Periode(bosted.fraOgMed, bosted.tilOgMed)] = Bosteder.BostedPeriodeInfo(Landkode.of(bosted.landkode))
    }

    utenlandsoppholdNeste12Mnd.forEach { bosted ->
        perioder[Periode(bosted.fraOgMed, bosted.tilOgMed)] = Bosteder.BostedPeriodeInfo(Landkode.of(bosted.landkode))
    }

    return Bosteder(perioder)
}

private fun UtenlandsoppholdIPerioden.tilK9Utenlandsopphold(
    periode: Periode
): Utenlandsopphold? {
    if (!skalOppholdeSegIUtlandetIPerioden) return null

    val perioder = mutableMapOf<Periode, UtenlandsoppholdPeriodeInfo>()

    opphold.forEach {
        val årsak = when (it.årsak) {
            Årsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING -> BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING
            Årsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD -> BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD
            else -> null
        }

        perioder[periode] = UtenlandsoppholdPeriodeInfo.builder()
            .land(Landkode.of(it.landkode))
            .årsak(årsak)
            .build()
    }

    return Utenlandsopphold(perioder)
}

fun MeldingV1.byggK9DataBruktTilUtledning(): DataBruktTilUtledning = DataBruktTilUtledning(
    harForståttRettigheterOgPlikter,
    harBekreftetOpplysninger,
    samtidigHjemme,
    harMedsøker,
    bekrefterPeriodeOver8Uker
)

fun PreprossesertMeldingV1.byggK9DataBruktTilUtledning(): DataBruktTilUtledning = DataBruktTilUtledning(
    harForstattRettigheterOgPlikter,
    harBekreftetOpplysninger,
    samtidigHjemme,
    harMedsøker,
    bekrefterPeriodeOver8Uker
)

fun MeldingV1.byggK9Uttak(periode: Periode): Uttak? {
    val perioder = mutableMapOf<Periode, UttakPeriodeInfo>()

    perioder[periode] = UttakPeriodeInfo(Duration.ofHours(7).plusMinutes(30))

    return Uttak(perioder)
}

fun PreprossesertMeldingV1.byggK9Uttak(periode: Periode): Uttak? {
    val perioder = mutableMapOf<Periode, UttakPeriodeInfo>()

    perioder[periode] = UttakPeriodeInfo(Duration.ofHours(7).plusMinutes(30))

    return Uttak(perioder)
}
