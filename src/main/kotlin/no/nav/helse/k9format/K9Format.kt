package no.nav.helse.k9format

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.prosessering.v1.*
import no.nav.helse.prosessering.v1.Tilsynsordning
import no.nav.k9.søknad.JsonUtils.getObjectMapper
import no.nav.k9.søknad.felles.*
import no.nav.k9.søknad.felles.Barn
import no.nav.k9.søknad.felles.Søker
import no.nav.k9.søknad.pleiepengerbarn.*
import no.nav.k9.søknad.pleiepengerbarn.Beredskap
import no.nav.k9.søknad.pleiepengerbarn.Nattevåk
import no.nav.k9.søknad.pleiepengerbarn.Utenlandsopphold
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate

fun PreprossesertMeldingV1.tilK9PleiepengeBarnSøknad(): JsonNode {
    val språk = when (språk) {
        "nb" -> Språk.NORSK_BOKMÅL
        "nn" -> Språk.NORSK_NYNORSK
        else -> Språk.NORSK_BOKMÅL
    }
    val builder = PleiepengerBarnSøknad.builder()
        .søknadId(SøknadId.of(søknadId))
        .mottattDato(mottatt)
        .språk(språk)
        .søker(søker.tilK9Søker())
        .arbeid(
            arbeidsgivere.tilK9Arbeid(
                frilans,
                selvstendigVirksomheter,
                Periode.builder().fraOgMed(fraOgMed).tilOgMed(tilOgMed).build()
            )
        )
        .barn(barn.tilK9Barn())
        .søknadsperiode(Periode.builder().fraOgMed(fraOgMed).tilOgMed(tilOgMed).build(), SøknadsperiodeInfo())

    builder.bosteder(medlemskap.tilK9bosteder())

    beredskap?.let {
        builder.beredskap(
            Beredskap.builder().periode(
                Periode.builder().fraOgMed(fraOgMed).tilOgMed(tilOgMed).build(),
                Beredskap.BeredskapPeriodeInfo.builder().tilleggsinformasjon(beredskap.tilleggsinformasjon).build()
            ).build()
        )
    }

    utenlandsoppholdIPerioden?.let {
        builder.utenlandsopphold(utenlandsoppholdIPerioden.tilK9Utenlandsopphold())
    }

    nattevåk?.let {
        val nattvåkBuilder = Nattevåk.NattevåkPeriodeInfo.builder()
        it.tilleggsinformasjon?.let { nattvåkBuilder.tilleggsinformasjon(it) }
        builder.nattevåk(
            Nattevåk.builder()
                .periode(
                    Periode.builder().fraOgMed(fraOgMed).tilOgMed(tilOgMed).build(),
                    nattvåkBuilder.build()
                )
                .build()
        )
    }

    if (tilsynsordning != null) builder.tilsynsordning(tilsynsordning.tilK9Tilsynsordning(fraOgMed, tilOgMed))
    else builder.tilsynsordning(
        no.nav.k9.søknad.pleiepengerbarn.Tilsynsordning.builder().iTilsynsordning(TilsynsordningSvar.NEI).build()
    )


    ferieuttakIPerioden?.let {
        if (ferieuttakIPerioden.skalTaUtFerieIPerioden) {
            builder.lovbestemtFerie(ferieuttakIPerioden.tilK9LovbestemtFerie())
        }
    }
    return getObjectMapper().readTree(PleiepengerBarnSøknad.SerDes.serialize(builder.build()))
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

    val utenlandsoppholdSiste12MndTilOgMed = mutableSetOf<LocalDate>()

    utenlandsoppholdSiste12Mnd.forEach {
        utenlandsoppholdSiste12MndTilOgMed.add(it.tilOgMed)
        val periode = Periode.builder().fraOgMed(it.fraOgMed).tilOgMed(it.tilOgMed).build()
        perioder[periode] =
            Bosteder.BostedPeriodeInfo.builder().land(Landkode.of(it.landkode)).build()
    }

    utenlandsoppholdNeste12Mnd.forEach {
        val fraOgMed =
            if (utenlandsoppholdSiste12MndTilOgMed.contains(it.fraOgMed)) it.fraOgMed.plusDays(1) else it.fraOgMed
        val tilOgMed = if (it.fraOgMed == it.tilOgMed) fraOgMed else it.tilOgMed
        val periode = Periode.builder().fraOgMed(fraOgMed).tilOgMed(tilOgMed).build()
        perioder[periode] =
            Bosteder.BostedPeriodeInfo.builder().land(Landkode.of(it.landkode)).build()
    }

    return Bosteder.builder()
        .perioder(perioder)
        .build()
}

fun Tilsynsordning.tilK9Tilsynsordning(
    fraOgMed: LocalDate,
    tilOgMed: LocalDate
): no.nav.k9.søknad.pleiepengerbarn.Tilsynsordning {
    val builder = no.nav.k9.søknad.pleiepengerbarn.Tilsynsordning.builder()
    val tilsynsordningSvar = when (svar) {
        "ja" -> TilsynsordningSvar.JA
        "nei" -> TilsynsordningSvar.NEI
        "vetIkke" -> TilsynsordningSvar.VET_IKKE
        else -> throw IllegalArgumentException("Ikke gyldig tilsynsordningsvar. Forventet ja/nei, men fikk $svar")
    }
    return when {
        this.ja != null -> {
            builder
                .iTilsynsordning(tilsynsordningSvar)
                .uke(this.ja.tilK9TilsynsordningUke(fraOgMed, tilOgMed))
                .build()
        }
        this.vetIkke != null -> {
            builder.iTilsynsordning(tilsynsordningSvar).build()
        }
        else -> builder.iTilsynsordning(tilsynsordningSvar).build()
    }
}

fun TilsynsordningJa.tilK9TilsynsordningUke(fraOgMed: LocalDate, tilOgMed: LocalDate): TilsynsordningUke {
    val builder = TilsynsordningUke.builder()
    mandag?.let { builder.mandag(mandag) }
    tirsdag?.let { builder.tirsdag(tirsdag) }
    onsdag?.let { builder.onsdag(onsdag) }
    torsdag?.let { builder.torsdag(torsdag) }
    fredag?.let { builder.fredag(fredag) }

    return builder
        .periode(Periode.builder().fraOgMed(fraOgMed).tilOgMed(tilOgMed).build())
        .build()
}

fun UtenlandsoppholdIPerioden.tilK9Utenlandsopphold(): Utenlandsopphold? {
    val perioder = mutableMapOf<Periode, Utenlandsopphold.UtenlandsoppholdPeriodeInfo>()
    opphold.forEach {
        val årsak = when (it.årsak) {
            Årsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING -> Utenlandsopphold.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING
            Årsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD -> Utenlandsopphold.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD
            else -> null
        }

        perioder.put(
            Periode.builder().fraOgMed(it.fraOgMed).tilOgMed(it.tilOgMed).build(),
            Utenlandsopphold.UtenlandsoppholdPeriodeInfo.builder()
                .land(Landkode.of(it.landkode))
                .årsak(årsak)
                .build()
        )
    }
    return Utenlandsopphold.builder()
        .perioder(perioder)
        .build()
}

fun PreprossesertBarn.tilK9Barn(): Barn {
    return when {
        !fødselsnummer.isNullOrBlank() -> Barn.builder().norskIdentitetsnummer(NorskIdentitetsnummer.of(fødselsnummer))
            .build()
        else -> Barn.builder().fødselsdato(fødselsdato).build()
    }
}

fun PreprossesertSøker.tilK9Søker(): Søker = Søker.builder()
    .norskIdentitetsnummer(NorskIdentitetsnummer.of(fødselsnummer))
    .build()

fun Arbeidsgivere.tilK9Arbeid(
    frilans: Frilans?,
    selvstendigVirksomheter: List<Virksomhet>?,
    søknadsPeriode: Periode
): Arbeid {

    val builder = Arbeid.builder()
        .arbeidstaker(organisasjoner.map { org ->
            Arbeidstaker.builder()
                .organisasjonsnummer(Organisasjonsnummer.of(org.organisasjonsnummer))
                .periode(
                    søknadsPeriode,
                    Arbeidstaker.ArbeidstakerPeriodeInfo.builder()
                        .skalJobbeProsent(BigDecimal.valueOf(org.skalJobbeProsent))
                        .jobberNormaltPerUke(org.jobberNormaltTimer.timerTilDuration())
                        .build()
                )
                .build()
        }.toMutableList())

    frilans?.let {
        builder.frilanser(frilans.tilK9Frilanser())
    }

    selvstendigVirksomheter?.let {
        builder.selvstendigNæringsdrivende(selvstendigVirksomheter.tilK9SelvstendigNæringsdrivende())
    }


    return builder.build()
}

private fun List<Virksomhet>.tilK9SelvstendigNæringsdrivende(): SelvstendigNæringsdrivende {
    val perioder = mutableMapOf<Periode, SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo>()
    map {
        perioder.put(
            Periode.builder().fraOgMed(it.fraOgMed).tilOgMed(it.tilOgMed).build(),
            SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo()
        )
    }
    return SelvstendigNæringsdrivende.builder().perioder(perioder).build()
}

private fun Frilans.tilK9Frilanser(): Frilanser {
    return Frilanser.builder()
        .periode(Periode.builder().fraOgMed(this.startdato).build(), Frilanser.FrilanserPeriodeInfo())
        .build()
}
