@file:Suppress("SpellCheckingInspection")

package no.nav.helse.k9format

import no.nav.helse.felles.*
import no.nav.helse.prosessering.v1.PreprossesertMeldingV1
import no.nav.helse.prosessering.v2.PreprossesertMeldingV2
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.LovbestemtFerie
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.aktivitet.*
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
import no.nav.k9.søknad.ytelse.psb.v1.UttakPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidInfo
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.TilsynPeriodeInfo
import java.time.Duration

fun PreprossesertMeldingV1.tilK9PleiepengesøknadSyktBarn(): Søknad {
    val søknadsPeriode = Periode(fraOgMed, tilOgMed)
    val søknad = Søknad(
        SøknadId.of(søknadId),
        Versjon.of("2.0"),
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

fun PreprossesertBarn.tilK9Barn(): Barn = Barn.builder()
    .norskIdentitetsnummer(NorskIdentitetsnummer.of(this.fødselsnummer))
    .fødselsdato(this.fødselsdato)
    .build()

fun PreprossesertSøker.tilK9Søker(): Søker = Søker.builder()
    .norskIdentitetsnummer(NorskIdentitetsnummer.of(fødselsnummer))
    .build()

fun PreprossesertMeldingV1.byggK9ArbeidAktivitet(): ArbeidAktivitet {
    val builder = ArbeidAktivitet.builder()

    frilans?.let {
        builder.frilanser(frilans.tilK9Frilanser())
    }

    builder.selvstendigNæringsdrivende(selvstendigVirksomheter.tilK9SelvstendigNæringsdrivende())
    builder.arbeidstaker(arbeidsgivere.tilK9Arbeidstaker(søker.fødselsnummer, Periode(fraOgMed, tilOgMed)))

    return builder.build()
}

fun Frilans.tilK9Frilanser(): Frilanser = Frilanser.builder()
    .jobberFortsattSomFrilans(this.jobberFortsattSomFrilans)
    .startdato(this.startdato)
    .build()

fun List<Virksomhet>.tilK9SelvstendigNæringsdrivende(): List<SelvstendigNæringsdrivende> = map { virksomhet ->
    SelvstendigNæringsdrivende.builder()
        .organisasjonsnummer(Organisasjonsnummer.of(virksomhet.organisasjonsnummer))
        .virksomhetNavn(virksomhet.navnPåVirksomheten)
        .periode(
            Periode(virksomhet.fraOgMed, virksomhet.tilOgMed),
            virksomhet.tilK9SelvstendingNæringsdrivendeInfo()
        )
        .build()
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

    infoBuilder.erNyoppstartet(true) //TODO Må sjekke hva som er riktig her
    yrkesaktivSisteTreFerdigliknedeÅrene?.let {
        infoBuilder.erNyoppstartet(false)
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
): no.nav.k9.søknad.ytelse.psb.v1.Beredskap? {
    if (!beredskap) return null

    val perioder = mutableMapOf<Periode, BeredskapPeriodeInfo>()

    perioder[periode] = BeredskapPeriodeInfo(this.tilleggsinformasjon)

    return no.nav.k9.søknad.ytelse.psb.v1.Beredskap(perioder)
}

fun Nattevåk.tilK9Nattevåk(periode: Periode): no.nav.k9.søknad.ytelse.psb.v1.Nattevåk? {
    if (!harNattevåk) return null

    val perioder = mutableMapOf<Periode, NattevåkPeriodeInfo>()

    perioder[periode] = NattevåkPeriodeInfo(tilleggsinformasjon)

    return no.nav.k9.søknad.ytelse.psb.v1.Nattevåk(perioder)
}

private fun Tilsynsordning.tilK9Tilsynsordning(
    periode: Periode
): no.nav.k9.søknad.ytelse.psb.v1.tilsyn.Tilsynsordning {
    val perioder = mutableMapOf<Periode, TilsynPeriodeInfo>()

    perioder[periode] = TilsynPeriodeInfo(
        Duration.ofHours(4) //TODO Har ikke dette. Skal man hente inn for man-fre også dele på 5?
    )

    return no.nav.k9.søknad.ytelse.psb.v1.tilsyn.Tilsynsordning(perioder)
}

private fun FerieuttakIPerioden.tilK9LovbestemtFerie(): LovbestemtFerie? {
    if (!skalTaUtFerieIPerioden) return null

    val perioder = mutableListOf<Periode>()

    ferieuttak.forEach { ferieuttak ->
        perioder.add(Periode(ferieuttak.fraOgMed, ferieuttak.tilOgMed))
    }

    return LovbestemtFerie(perioder)
}

fun Medlemskap.tilK9Bosteder(): Bosteder {
    val perioder = mutableMapOf<Periode, Bosteder.BostedPeriodeInfo>()

    utenlandsoppholdSiste12Mnd.forEach { bosted ->
        perioder[Periode(bosted.fraOgMed, bosted.tilOgMed)] = Bosteder.BostedPeriodeInfo.builder()
            .land(Landkode.of(bosted.landkode))
            .build()
    }

    utenlandsoppholdNeste12Mnd.forEach { bosted ->
        perioder[Periode(bosted.fraOgMed, bosted.tilOgMed)] = Bosteder.BostedPeriodeInfo.builder()
            .land(Landkode.of(bosted.landkode))
            .build()
    }

    return Bosteder.builder().perioder(perioder).build()
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

    return Utenlandsopphold.builder().perioder(perioder).build()
}

fun PreprossesertMeldingV1.byggSøknadInfo(): SøknadInfo = SøknadInfo(
    barnRelasjon?.utskriftsvennlig ?: "Forelder",
    skalBekrefteOmsorg,
    beskrivelseOmsorgsrollen,
    harForstattRettigheterOgPlikter,
    harBekreftetOpplysninger,
    false, //TODO Mangler dette feltet,
    samtidigHjemme,
    harMedsøker,
    bekrefterPeriodeOver8Uker
)

fun Organisasjon.tilK9ArbeidstidInfo(periode: Periode): ArbeidstidInfo {
    val perioder = mutableMapOf<Periode, ArbeidstidPeriodeInfo>()

    perioder[periode] = ArbeidstidPeriodeInfo(
        Duration.ofHours(6) //TODO Mangler denne verdien
    )
    return ArbeidstidInfo(Duration.ofHours(7), perioder) //TODO Mangler denne verdien
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
        Duration.ofHours(5) //TODO Mangler denne verdien
    )

    return ArbeidstidInfo(Duration.ofHours(7), perioder) //TODO Mangler denne verdien
}

fun List<Virksomhet>.tilK9ArbeidstidInfo(): ArbeidstidInfo {
    val perioder = mutableMapOf<Periode, ArbeidstidPeriodeInfo>()

    forEach { virksomhet ->
        perioder[Periode(virksomhet.fraOgMed, virksomhet.tilOgMed)] =
                //TODO Er dette riktig å bruke periode fra virksomheten eller periode for søknadsperioden
            ArbeidstidPeriodeInfo(Duration.ofHours(4)) //TODO Mangler denne verdien
    }

    return ArbeidstidInfo(Duration.ofHours(7), perioder) //TODO Mangler denne verdien
}

fun PreprossesertMeldingV2.byggK9Uttak(periode: Periode): Uttak {
    val perioder = mutableMapOf<Periode, UttakPeriodeInfo>()

    perioder[periode] = UttakPeriodeInfo(Duration.ofHours(5)) //TODO Mangler info om dette

    return Uttak(perioder)
}

fun PreprossesertMeldingV1.byggK9Uttak(periode: Periode): Uttak {
    val perioder = mutableMapOf<Periode, UttakPeriodeInfo>()

    perioder[periode] = UttakPeriodeInfo(Duration.ofHours(5)) //TODO Mangler info om dette

    return Uttak(perioder)
}
