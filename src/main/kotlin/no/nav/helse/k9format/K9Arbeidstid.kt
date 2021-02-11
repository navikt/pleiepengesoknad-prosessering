package no.nav.helse.k9format

import no.nav.helse.felles.Frilans
import no.nav.helse.felles.Organisasjon
import no.nav.helse.felles.Virksomhet
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.PreprossesertMeldingV1
import no.nav.k9.søknad.felles.aktivitet.Arbeidstaker
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidInfo
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidPeriodeInfo
import java.time.Duration

fun MeldingV1.byggK9Arbeidstid(): Arbeidstid {
    val frilanserArbeidstidInfo = null //frilans?.tilK9ArbeidstidInfo(Periode(fraOgMed, tilOgMed)) //TODO 08.02.2021 - Når vi har nok info
    val selvstendigNæringsdrivendeArbeidstidInfo = null //selvstendigVirksomheter.tilK9ArbeidstidInfo() //TODO 08.02.2021 - Når vi har nok info
    val arbeidstakerList: List<Arbeidstaker> = arbeidsgivere.tilK9Arbeidstaker(søker.fødselsnummer, Periode(fraOgMed, tilOgMed))

    return Arbeidstid(arbeidstakerList, frilanserArbeidstidInfo, selvstendigNæringsdrivendeArbeidstidInfo)
}

fun PreprossesertMeldingV1.byggK9Arbeidstid(): Arbeidstid {
    val frilanserArbeidstidInfo = null //frilans?.tilK9ArbeidstidInfo(Periode(fraOgMed, tilOgMed)) //TODO 08.02.2021 - Når vi har nok info
    val selvstendigNæringsdrivendeArbeidstidInfo = null //selvstendigVirksomheter.tilK9ArbeidstidInfo() //TODO 08.02.2021 - Når vi har nok info
    val arbeidstakerList: List<Arbeidstaker> = arbeidsgivere.tilK9Arbeidstaker(søker.fødselsnummer, Periode(fraOgMed, tilOgMed))

    return Arbeidstid(arbeidstakerList, frilanserArbeidstidInfo, selvstendigNæringsdrivendeArbeidstidInfo)
}

fun Organisasjon.tilK9ArbeidstidInfo(periode: Periode): ArbeidstidInfo {
    val perioder = mutableMapOf<Periode, ArbeidstidPeriodeInfo>()

    val faktiskTimerPerUke = jobberNormaltTimer.tilFaktiskTimerPerUke(skalJobbeProsent)
    val normalTimerPerDag = jobberNormaltTimer.tilTimerPerDag().tilDuration()
    val faktiskArbeidstimerPerDag = faktiskTimerPerUke.tilTimerPerDag().tilDuration()

    perioder[periode] = ArbeidstidPeriodeInfo(faktiskArbeidstimerPerDag)

    return ArbeidstidInfo(normalTimerPerDag, perioder)
}

fun Double.tilDuration() = Duration.ofMinutes((this * 60).toLong())


fun Frilans.tilK9ArbeidstidInfo(periode: Periode): ArbeidstidInfo {
    val perioder = mutableMapOf<Periode, ArbeidstidPeriodeInfo>()

    perioder[periode] = ArbeidstidPeriodeInfo(null) //TODO Mangler denne verdien i brukerdialog

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

fun Double.tilFaktiskTimerPerUke(prosent: Double) = this.times(prosent.div(100))
fun Double.tilTimerPerDag() = this.div(DAGER_PER_UKE)