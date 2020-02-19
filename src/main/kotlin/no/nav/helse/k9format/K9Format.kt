package no.nav.helse.k9format

import no.nav.helse.prosessering.v1.*
import no.nav.helse.prosessering.v1.Tilsynsordning
import no.nav.k9.søknad.felles.*
import no.nav.k9.søknad.felles.Barn
import no.nav.k9.søknad.pleiepengerbarn.*
import no.nav.k9.søknad.pleiepengerbarn.Beredskap
import no.nav.k9.søknad.pleiepengerbarn.Utenlandsopphold
import java.math.BigDecimal

fun PreprossesertMeldingV1.tilK9PleiepengeBarnSøknad(): PleiepengerBarnSøknad {
    val builder = PleiepengerBarnSøknad.builder()
        .søknadId(SøknadId.of(soknadId))
        .mottattDato(mottatt)
        .språk(Språk.valueOf(sprak ?: "nb"))
        .søker(soker.tilK9Søker())
        .arbeid(
            arbeidsgivere.tilK9Arbeid(
                Periode.builder().fraOgMed(fraOgMed).tilOgMed(tilOgMed).build(),
                NorskIdentitetsnummer.of(soker.fodselsnummer)
            )
        )
        .barn(barn.tilK9Barn())

    builder.bosteder(medlemskap.tilK9bosteder())

    beredskap?.let {
        builder.beredskap(
            Beredskap.builder().periode(
                Periode.builder().fraOgMed(fraOgMed).tilOgMed(tilOgMed).build(),
                Beredskap.BeredskapPeriodeInfo.builder().tilleggsinformasjon(beredskap.tilleggsinformasjon).build()
            ).build()
        )
    }

    utenlandsoppholdIPerioden?.let { oppholdIPerioden: UtenlandsoppholdIPerioden ->
        builder.utenlandsopphold(utenlandsoppholdIPerioden.tilK9Utenlandsopphold())
    }

    nattevaak?.let {
        builder.nattevåk(
            Nattevåk.builder()
                .periode(
                    Periode.builder().fraOgMed(fraOgMed).tilOgMed(tilOgMed).build(),
                    Nattevåk.NattevåkPeriodeInfo.builder().tilleggsinformasjon(it.tilleggsinformasjon ?: "").build()
                )
                .build()
        )
    }

    tilsynsordning?.let {
        builder.tilsynsordning(tilsynsordning.tilK9Tilsynsordning())
    }

    ferieuttakIPerioden?.let {
        if (ferieuttakIPerioden.skalTaUtFerieIPerioden) {
            builder.lovbestemtFerie(ferieuttakIPerioden.tilK9LovbestemtFerie())
        }
    }

    return builder.build()
}

private fun FerieuttakIPerioden.tilK9LovbestemtFerie(): LovbestemtFerie {

    val perioder = mutableMapOf<Periode, LovbestemtFerie.LovbestemtFeriePeriodeInfo>()
    ferieuttak.forEach {
        perioder.put(
            Periode.builder().fraOgMed(it.fraOgMed).tilOgMed(it.tilOgMed).build(),
            LovbestemtFerie.LovbestemtFeriePeriodeInfo()
        )
    }

    return LovbestemtFerie.builder()
        .perioder(perioder)
        .build()
}


fun Medlemskap.tilK9bosteder(): Bosteder {
    val perioder = mutableMapOf<Periode, Bosteder.BostedPeriodeInfo>()
    utenlandsoppholdNeste12Mnd.forEach {
        perioder.put(
            Periode.builder().fraOgMed(it.fraOgMed).tilOgMed(it.tilOgMed).build(),
            Bosteder.BostedPeriodeInfo.builder().land(Landkode.of(it.landkode)).build()
        )
    }
    utenlandsoppholdSiste12Mnd.forEach {
        perioder.put(
            Periode.builder().fraOgMed(it.fraOgMed).tilOgMed(it.tilOgMed).build(),
            Bosteder.BostedPeriodeInfo.builder().land(Landkode.of(it.landkode)).build()
        )
    }
    return Bosteder.builder()
        .perioder(perioder)
        .build()
}

fun Tilsynsordning.tilK9Tilsynsordning(): no.nav.k9.søknad.pleiepengerbarn.Tilsynsordning {
    val builder = no.nav.k9.søknad.pleiepengerbarn.Tilsynsordning.builder()
    return when {
        this.ja != null -> {
            builder
                .iTilsynsordning(TilsynsordningSvar.valueOf(svar))
                .uke(this.ja.tilK9TilsynsordningUke()).build()
        }
        this.vetIkke != null -> {
            builder.iTilsynsordning(TilsynsordningSvar.valueOf(svar)).build()
        }
        else -> builder.iTilsynsordning(TilsynsordningSvar.valueOf(svar)).build()
    }
}

fun TilsynsordningJa.tilK9TilsynsordningUke(): TilsynsordningUke {
    val builder = TilsynsordningUke.builder()
    mandag?.let { builder.mandag(mandag) }
    tirsdag?.let { builder.tirsdag(tirsdag) }
    onsdag?.let { builder.onsdag(onsdag) }
    torsdag?.let { builder.torsdag(torsdag) }
    fredag?.let { builder.fredag(fredag) }

    return builder.build()
}

fun UtenlandsoppholdIPerioden.tilK9Utenlandsopphold(): Utenlandsopphold? {
    val perioder = mutableMapOf<Periode, Utenlandsopphold.UtenlandsoppholdPeriodeInfo>()
    opphold.forEach {
        perioder.put(
            Periode.builder().fraOgMed(it.fraOgMed).tilOgMed(it.tilOgMed).build(),
            Utenlandsopphold.UtenlandsoppholdPeriodeInfo.builder()
                .land(Landkode.of(it.landkode))
                .build()
        )
    }
    return Utenlandsopphold.builder()
        .perioder(perioder)
        .build()
}

fun PreprossesertBarn.tilK9Barn(): Barn {
    return when {
        !fodselsnummer.isNullOrBlank() -> Barn.builder().norskIdentitetsnummer(NorskIdentitetsnummer.of(fodselsnummer)).build()
        else -> Barn.builder().fødselsdato(fodselsdato).build()
    }
}

fun PreprossesertSoker.tilK9Søker(): Søker = Søker.builder()
    .norskIdentitetsnummer(NorskIdentitetsnummer.of(fodselsnummer))
    .build()

fun Arbeidsgivere.tilK9Arbeid(
    søknadsPeriode: Periode,
    norskIdentitetsnummer: NorskIdentitetsnummer
): Arbeid {

    return Arbeid.builder()
        .arbeidstaker(organisasjoner.map { org ->
            Arbeidstaker.builder()
                .norskIdentitetsnummer(norskIdentitetsnummer)
                .organisasjonsnummer(Organisasjonsnummer.of(org.organisasjonsnummer))
                .periode(
                    søknadsPeriode,
                    Arbeidstaker.ArbeidstakerPeriodeInfo.builder()
                        .skalJobbeProsent(BigDecimal.valueOf(org.skalJobbeProsent ?: 0.0))
                        .build()
                )
                .build()
        }.toMutableList())
        .build()
}
