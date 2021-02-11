package no.nav.helse.k9format

import no.nav.helse.felles.Arbeidsgivere
import no.nav.helse.felles.Næringstyper
import no.nav.helse.felles.Virksomhet
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.PreprossesertMeldingV1
import no.nav.k9.søknad.felles.aktivitet.*
import no.nav.k9.søknad.felles.type.Landkode
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Periode
import java.time.LocalDate

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

fun Arbeidsgivere.tilK9Arbeidstaker(
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