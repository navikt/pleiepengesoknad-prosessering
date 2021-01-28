@file:Suppress("SpellCheckingInspection")

package no.nav.helse.k9format

import no.nav.helse.prosessering.v1.*
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.LovbestemtFerie
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.aktivitet.*
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Bosteder
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING
import no.nav.k9.søknad.felles.type.Landkode
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.felles.type.SøknadId
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.TilsynsordningOpphold
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.TilsynsordningSvar
import java.time.Duration
import java.time.LocalDate

fun PreprossesertMeldingV1.tilK9PleiepengesøknadSyktBarn(): Søknad {
    val søknad = Søknad(
        SøknadId.of(søknadId),
        Versjon.of("2.0"),
        mottatt,
        søker.tilK9Søker(),
        PleiepengerSyktBarn(
            Periode(fraOgMed, tilOgMed),
            barn.tilK9Barn(),
            byggK9ArbeidAktivitet(),
            beredskap?.tilK9Beredskap(fraOgMed, tilOgMed),
            nattevåk?.tilK9Nattevåk(fraOgMed, tilOgMed),
            tilsynsordning?.tilK9Tilsynsordning(fraOgMed, tilOgMed),
            null, //TODO Arbeid er fjernet fra 5.1.7 versjonen
            null, //TODO Uttak
            ferieuttakIPerioden?.tilK9LovbestemtFerie(),
            medlemskap.tilK9Bosteder(),
            utenlandsoppholdIPerioden.tilK9Utenlandsopphold(fraOgMed, tilOgMed),
            null, // TODO flereOmsorgspersjoner fjernes i 5.1.7,
            null, //TODO Relasjon til barnet fjernes i 5.1.7
            null, //TODO samtykketOmsorgForBarnet fjernes i 5.1.7
            null //TODO veskrivelseavOmsorgsrollen fjernes i 5.1.7
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

    selvstendigVirksomheter?.let {
        builder.selvstendigNæringsdrivende(selvstendigVirksomheter.tilK9SelvstendigNæringsdrivende())
    }

    arbeidsgivere?.let {
        builder.arbeidstaker(arbeidsgivere.tilK9Arbeidstaker(søker.fødselsnummer, fraOgMed, tilOgMed))
    }

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
    tilOgMed: LocalDate,
    fraOgMed: LocalDate
): List<Arbeidstaker> {
    return organisasjoner.map { organisasjon ->
        Arbeidstaker.builder()
            .norskIdentitetsnummer(NorskIdentitetsnummer.of(identitetsnummer))
            .organisasjonsnummer(Organisasjonsnummer.of(organisasjon.organisasjonsnummer))
            .periode(Periode(fraOgMed, tilOgMed), organisasjon.tilK9ArbeidstakerPeriodeInfo())
            .build()
    }
}

private fun Organisasjon.tilK9ArbeidstakerPeriodeInfo(): Arbeidstaker.ArbeidstakerPeriodeInfo =
    Arbeidstaker.ArbeidstakerPeriodeInfo.builder()
        .skalJobbeProsent(skalJobbeProsent.toBigDecimal())
        .jobberNormaltPerUke(jobberNormaltTimer.timerTilDuration())
        .build()

private fun Beredskap.tilK9Beredskap(
    fraOgMed: LocalDate,
    tilOgMed: LocalDate
): no.nav.k9.søknad.ytelse.psb.v1.Beredskap? {
    if (!beredskap) return null

    val perioder = mutableMapOf<Periode, no.nav.k9.søknad.ytelse.psb.v1.Beredskap.BeredskapPeriodeInfo>()

    perioder.put(
        Periode(fraOgMed, tilOgMed),
        no.nav.k9.søknad.ytelse.psb.v1.Beredskap.BeredskapPeriodeInfo(this.tilleggsinformasjon)
    )

    return no.nav.k9.søknad.ytelse.psb.v1.Beredskap(perioder)
}

private fun Nattevåk.tilK9Nattevåk(fraOgMed: LocalDate, tilOgMed: LocalDate): no.nav.k9.søknad.ytelse.psb.v1.Nattevåk? {
    if (!harNattevåk) return null

    val perioder = mutableMapOf<Periode, no.nav.k9.søknad.ytelse.psb.v1.Nattevåk.NattevåkPeriodeInfo>()

    perioder.put(
        Periode(fraOgMed, tilOgMed),
        no.nav.k9.søknad.ytelse.psb.v1.Nattevåk.NattevåkPeriodeInfo(tilleggsinformasjon)
    )

    return no.nav.k9.søknad.ytelse.psb.v1.Nattevåk(perioder)
}

private fun Tilsynsordning.tilK9Tilsynsordning(
    fraOgMed: LocalDate,
    tilOgMed: LocalDate
): no.nav.k9.søknad.ytelse.psb.v1.tilsyn.Tilsynsordning {
    val perioder = mutableMapOf<Periode, TilsynsordningOpphold>()

    perioder.put(
        Periode(fraOgMed, tilOgMed),
        TilsynsordningOpphold(Duration.ofHours(1))
    ) //TODO Lengde blir fjernet i 5.1.7. Dette må ryddes bort

    val tilsynsordningSvar = when (svar) {
        "ja" -> TilsynsordningSvar.JA
        "nei" -> TilsynsordningSvar.NEI
        "vetIkke" -> TilsynsordningSvar.VET_IKKE
        "vet_ikke" -> TilsynsordningSvar.VET_IKKE
        else -> throw IllegalArgumentException("Ikke gyldig tilsynsordningsvar. Forventet ja/nei/vetIkke/vet_ikke, men fikk $svar")
    }

    return no.nav.k9.søknad.ytelse.psb.v1.tilsyn.Tilsynsordning(tilsynsordningSvar, perioder)
}

private fun FerieuttakIPerioden.tilK9LovbestemtFerie(): LovbestemtFerie? {
    if(!skalTaUtFerieIPerioden) return null

    val perioder = mutableListOf<Periode>()

    ferieuttak.forEach { ferieuttak ->
        perioder.add(Periode(ferieuttak.fraOgMed, ferieuttak.tilOgMed))
    }

    return LovbestemtFerie(perioder)
}

private fun Medlemskap.tilK9Bosteder(): Bosteder {
    val perioder = mutableMapOf<Periode, Bosteder.BostedPeriodeInfo>()

    utenlandsoppholdSiste12Mnd?.forEach { bosted ->
        perioder[Periode(bosted.fraOgMed, bosted.tilOgMed)] = Bosteder.BostedPeriodeInfo.builder()
            .land(Landkode.of(bosted.landkode))
            .build()
    }

    utenlandsoppholdNeste12Mnd?.forEach { bosted ->
        perioder.put(
            Periode(bosted.fraOgMed, bosted.tilOgMed),
            Bosteder.BostedPeriodeInfo.builder()
                .land(Landkode.of(bosted.landkode))
                .build()
        )
    }

    return Bosteder.builder().perioder(perioder).build()
}

private fun UtenlandsoppholdIPerioden.tilK9Utenlandsopphold(fraOgMed: LocalDate, tilOgMed: LocalDate): Utenlandsopphold? {
    if(!skalOppholdeSegIUtlandetIPerioden) return null

    val perioder = mutableMapOf<Periode, Utenlandsopphold.UtenlandsoppholdPeriodeInfo>()

    opphold.forEach {
        val årsak = when(it.årsak) {
            Årsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING -> BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING
            Årsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD -> BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD
            else -> null
        }

        perioder.put(
            Periode(fraOgMed, tilOgMed),
            Utenlandsopphold.UtenlandsoppholdPeriodeInfo.builder()
                .land(Landkode.of(it.landkode))
                .årsak(årsak)
                .build()
        )
    }

    return Utenlandsopphold.builder().perioder(perioder).build()
}