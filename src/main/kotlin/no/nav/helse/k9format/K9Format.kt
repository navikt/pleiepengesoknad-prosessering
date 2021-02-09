@file:Suppress("SpellCheckingInspection")

package no.nav.helse.k9format

import no.nav.helse.felles.Arbeidsgivere
import no.nav.helse.felles.Beredskap
import no.nav.helse.felles.FerieuttakIPerioden
import no.nav.helse.felles.Frilans
import no.nav.helse.felles.Medlemskap
import no.nav.helse.felles.Nattevåk
import no.nav.helse.felles.Næringstyper
import no.nav.helse.felles.Organisasjon
import no.nav.helse.felles.Tilsynsordning
import no.nav.helse.felles.UtenlandsoppholdIPerioden
import no.nav.helse.felles.Virksomhet
import no.nav.helse.felles.Årsak
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.PreprossesertMeldingV1
import no.nav.helse.prosessering.v1.snittTilsynsTimerPerDag
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.LovbestemtFerie
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.aktivitet.ArbeidAktivitet
import no.nav.k9.søknad.felles.aktivitet.Arbeidstaker
import no.nav.k9.søknad.felles.aktivitet.Frilanser
import no.nav.k9.søknad.felles.aktivitet.Organisasjonsnummer
import no.nav.k9.søknad.felles.aktivitet.SelvstendigNæringsdrivende
import no.nav.k9.søknad.felles.aktivitet.VirksomhetType
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Bosteder
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold.UtenlandsoppholdPeriodeInfo
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING
import no.nav.k9.søknad.felles.type.Landkode
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.felles.type.SøknadId
import no.nav.k9.søknad.ytelse.psb.v1.Beredskap.BeredskapPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.Nattevåk.NattevåkPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9.søknad.ytelse.psb.v1.SøknadInfo
import no.nav.k9.søknad.ytelse.psb.v1.Uttak
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidInfo
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.TilsynPeriodeInfo
import java.time.Duration
import java.time.LocalDate
import no.nav.k9.søknad.ytelse.psb.v1.Beredskap as K9Beredskap

const val DAGER_PER_UKE = 5

private val k9FormatVersjon = Versjon.of("1.0")

fun MeldingV1.tilK9PleiepengesøknadSyktBarn(): Søknad {
    val søknadsPeriode = Periode(fraOgMed, tilOgMed)
    val søknad = Søknad(
        SøknadId.of(søknadId),
        k9FormatVersjon,
        mottatt,
        søker.tilK9Søker(),
        PleiepengerSyktBarn(
            søknadsPeriode,
            byggSøknadInfo(),
            barn.tilK9Barn(),
            byggK9ArbeidAktivitet(),
            beredskap?.tilK9Beredskap(søknadsPeriode),
            nattevåk?.tilK9Nattevåk(søknadsPeriode),
            tilsynsordning?.tilK9Tilsynsordning(søknadsPeriode),
            byggK9Arbeidstid(),
            byggK9Uttak(søknadsPeriode),
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
            byggSøknadInfo(),
            barn.tilK9Barn(),
            byggK9ArbeidAktivitet(),
            beredskap?.tilK9Beredskap(søknadsPeriode),
            nattevåk?.tilK9Nattevåk(søknadsPeriode),
            tilsynsordning?.tilK9Tilsynsordning(søknadsPeriode),
            byggK9Arbeidstid(),
            byggK9Uttak(søknadsPeriode),
            ferieuttakIPerioden?.tilK9LovbestemtFerie(),
            medlemskap.tilK9Bosteder(),
            utenlandsoppholdIPerioden.tilK9Utenlandsopphold(søknadsPeriode)
        )
    )
    return søknad
}

fun no.nav.helse.felles.Barn.tilK9Barn(): Barn = Barn(NorskIdentitetsnummer.of(this.fødselsnummer), (this.fødselsdato))

fun no.nav.helse.felles.PreprossesertBarn.tilK9Barn(): Barn =
    Barn(NorskIdentitetsnummer.of(this.fødselsnummer), (this.fødselsdato))

fun no.nav.helse.felles.Søker.tilK9Søker(): Søker = Søker(NorskIdentitetsnummer.of(fødselsnummer))

fun no.nav.helse.felles.PreprossesertSøker.tilK9Søker(): Søker = Søker(NorskIdentitetsnummer.of(fødselsnummer))

fun MeldingV1.byggK9ArbeidAktivitet(): ArbeidAktivitet = ArbeidAktivitet(
    arbeidsgivere.tilK9Arbeidstaker(søker.fødselsnummer, Periode(fraOgMed, tilOgMed)),
    selvstendigVirksomheter.tilK9SelvstendigNæringsdrivende(),
    frilans?.tilK9Frilanser()
)

fun PreprossesertMeldingV1.byggK9ArbeidAktivitet(): ArbeidAktivitet = ArbeidAktivitet(
    arbeidsgivere.tilK9Arbeidstaker(søker.fødselsnummer, Periode(fraOgMed, tilOgMed)),
    selvstendigVirksomheter.tilK9SelvstendigNæringsdrivende(),
    frilans?.tilK9Frilanser()
)

fun Frilans.tilK9Frilanser(): Frilanser = Frilanser(startdato, jobberFortsattSomFrilans)

fun List<Virksomhet>.tilK9SelvstendigNæringsdrivende(): List<SelvstendigNæringsdrivende> = map { virksomhet ->
    SelvstendigNæringsdrivende(
        mapOf(Periode(virksomhet.fraOgMed, virksomhet.tilOgMed) to virksomhet.tilK9SelvstendingNæringsdrivendeInfo()),
        Organisasjonsnummer.of(virksomhet.organisasjonsnummer),
        virksomhet.navnPåVirksomheten
    )
}

fun Virksomhet.tilK9SelvstendingNæringsdrivendeInfo(): SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo {
    val infoBuilder = SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo.builder()
    infoBuilder
        .virksomhetstyper(næringstyper.tilK9VirksomhetType())

    if (registrertINorge) {
        infoBuilder
            .landkode(Landkode.NORGE)
            .registrertIUtlandet(false)
    } else {
        infoBuilder
            .landkode(Landkode.of(registrertIUtlandet!!.landkode))
            .registrertIUtlandet(true)
    }

    when (erEldreEnn3År()) {
        true -> infoBuilder.erNyoppstartet(false)
        false -> infoBuilder.erNyoppstartet(true)
    }

    regnskapsfører?.let {
        infoBuilder
            .regnskapsførerNavn(regnskapsfører.navn)
            .regnskapsførerTelefon(regnskapsfører.telefon)
    }

    næringsinntekt?.let {
        infoBuilder
            .bruttoInntekt(næringsinntekt.toBigDecimal())
    }

    infoBuilder.erVarigEndring(false)
    varigEndring?.let {
        infoBuilder
            .erVarigEndring(true)
            .endringDato(it.dato)
            .endringBegrunnelse(it.forklaring)
    }

    return infoBuilder.build()
}

private fun Virksomhet.erEldreEnn3År() =
    fraOgMed.isBefore(LocalDate.now().minusYears(3)) || fraOgMed.isEqual(LocalDate.now().minusYears(3))

private fun List<Næringstyper>.tilK9VirksomhetType(): List<VirksomhetType> = map {
    when (it) {
        Næringstyper.FISKE -> VirksomhetType.FISKE
        Næringstyper.JORDBRUK_SKOGBRUK -> VirksomhetType.JORDBRUK_SKOGBRUK
        Næringstyper.DAGMAMMA -> VirksomhetType.DAGMAMMA
        Næringstyper.ANNEN -> VirksomhetType.ANNEN
    }
}

private fun Arbeidsgivere.tilK9Arbeidstaker(
    identitetsnummer: String,
    periode: Periode
): List<Arbeidstaker> {
    return organisasjoner.map { organisasjon ->
        Arbeidstaker(
            NorskIdentitetsnummer.of(identitetsnummer),
            Organisasjonsnummer.of(organisasjon.organisasjonsnummer),
            organisasjon.tilK9ArbeidstidInfo(periode)
        )
    }
}

fun Beredskap.tilK9Beredskap(
    periode: Periode
): K9Beredskap? =
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

fun MeldingV1.byggSøknadInfo(): SøknadInfo = SøknadInfo(
    barnRelasjon?.utskriftsvennlig ?: "Forelder",
    skalBekrefteOmsorg,
    beskrivelseOmsorgsrollen,
    harForståttRettigheterOgPlikter,
    harBekreftetOpplysninger,
    null,
    samtidigHjemme,
    harMedsøker,
    bekrefterPeriodeOver8Uker
)

fun PreprossesertMeldingV1.byggSøknadInfo(): SøknadInfo = SøknadInfo(
    barnRelasjon?.utskriftsvennlig ?: "Forelder",
    skalBekrefteOmsorg,
    beskrivelseOmsorgsrollen,
    harForstattRettigheterOgPlikter,
    harBekreftetOpplysninger,
    null,
    samtidigHjemme,
    harMedsøker,
    bekrefterPeriodeOver8Uker
)

fun Double.tilFaktiskTimerPerUke(prosent: Double) = this.times(prosent.div(100))
fun Double.tilTimerPerDag() = this.div(DAGER_PER_UKE)

fun Organisasjon.tilK9ArbeidstidInfo(periode: Periode): ArbeidstidInfo {
    val perioder = mutableMapOf<Periode, ArbeidstidPeriodeInfo>()

    val faktiskTimerPerUke = jobberNormaltTimer.tilFaktiskTimerPerUke(skalJobbeProsent)
    val normalTimerPerDag = jobberNormaltTimer.tilTimerPerDag().tilDuration()
    val faktiskArbeidstimerPerDag = faktiskTimerPerUke.tilTimerPerDag().tilDuration()

    perioder[periode] = ArbeidstidPeriodeInfo(faktiskArbeidstimerPerDag)

    return ArbeidstidInfo(normalTimerPerDag, perioder)
}

fun Double.tilDuration() = Duration.ofMinutes((this * 60).toLong())

fun MeldingV1.byggK9Arbeidstid(): Arbeidstid {
    val frilanserArbeidstidInfo = frilans?.tilK9ArbeidstidInfo(Periode(fraOgMed, tilOgMed))
    val selvstendigNæringsdrivendeArbeidstidInfo = selvstendigVirksomheter.tilK9ArbeidstidInfo()
    val arbeidstakerList: List<Arbeidstaker> =
        arbeidsgivere.tilK9Arbeidstaker(søker.fødselsnummer, Periode(fraOgMed, tilOgMed))

    return Arbeidstid(arbeidstakerList, frilanserArbeidstidInfo, selvstendigNæringsdrivendeArbeidstidInfo)
}

fun PreprossesertMeldingV1.byggK9Arbeidstid(): Arbeidstid {
    val frilanserArbeidstidInfo = frilans?.tilK9ArbeidstidInfo(Periode(fraOgMed, tilOgMed))
    val selvstendigNæringsdrivendeArbeidstidInfo = selvstendigVirksomheter.tilK9ArbeidstidInfo()
    val arbeidstakerList: List<Arbeidstaker> =
        arbeidsgivere.tilK9Arbeidstaker(søker.fødselsnummer, Periode(fraOgMed, tilOgMed))

    return Arbeidstid(arbeidstakerList, frilanserArbeidstidInfo, selvstendigNæringsdrivendeArbeidstidInfo)
}

fun Frilans.tilK9ArbeidstidInfo(periode: Periode): ArbeidstidInfo {
    val perioder = mutableMapOf<Periode, ArbeidstidPeriodeInfo>()

    perioder[periode] = ArbeidstidPeriodeInfo(
        null //TODO Mangler denne verdien i brukerdialog
    )

    return ArbeidstidInfo(null, perioder) //TODO Mangler denne verdien i brukerdialog
}

fun List<Virksomhet>.tilK9ArbeidstidInfo(): ArbeidstidInfo? {
    if (isEmpty()) return null
    val perioder = mutableMapOf<Periode, ArbeidstidPeriodeInfo>()

    forEach { virksomhet ->
        //TODO Er dette riktig å bruke periode fra virksomheten eller periode for søknadsperioden
        perioder[Periode(virksomhet.fraOgMed, virksomhet.tilOgMed)] =
            ArbeidstidPeriodeInfo(null) //TODO Mangler denne verdien i brukerdialog
    }

    return ArbeidstidInfo(null, perioder) //TODO Mangler denne verdien i brukerdialog
}

fun MeldingV1.byggK9Uttak(periode: Periode): Uttak? {
    return null
    /*val perioder = mutableMapOf<Periode, UttakPeriodeInfo>()

    perioder[periode] = UttakPeriodeInfo(null) //TODO Mangler info om dette i brukerdialog

    return Uttak(perioder)*/
}

fun PreprossesertMeldingV1.byggK9Uttak(periode: Periode): Uttak? {
    return null
    /*val perioder = mutableMapOf<Periode, UttakPeriodeInfo>()

    perioder[periode] = UttakPeriodeInfo(null) //TODO Mangler info om dette i brukerdialog

    return Uttak(perioder)*/
}
