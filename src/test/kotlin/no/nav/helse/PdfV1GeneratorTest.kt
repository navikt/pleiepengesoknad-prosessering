package no.nav.helse

import no.nav.helse.aktoer.Fodselsnummer
import no.nav.helse.prosessering.v1.*
import org.junit.Ignore
import java.io.File
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.Test

class PdfV1GeneratorTest {

    private companion object {
        private val generator = PdfV1Generator()
        private val barnetsIdent = Fodselsnummer("02119970078")
        private val barnetsNavn = "Ole Dole"
        private val fødselsdato = LocalDate.now().minusDays(10)
    }

    private fun fullGyldigMelding(soknadsId: String): MeldingV1 {
        return MeldingV1(
            språk = "nb",
            søknadId = soknadsId,
            mottatt = ZonedDateTime.now(),
            fraOgMed = LocalDate.now().plusDays(6),
            tilOgMed = LocalDate.now().plusDays(35),
            søker = Søker(
                aktørId = "123456",
                fornavn = "Ærling",
                mellomnavn = "Øverbø",
                etternavn = "Ånsnes",
                fødselsnummer = "29099012345"
            ),
            barn = Barn(
                fødselsnummer = barnetsIdent.getValue(),
                aktørId = "123456",
                navn = barnetsNavn,
                fødselsdato = null
            ),
            relasjonTilBarnet = "Mor",
            arbeidsgivere = Arbeidsgivere(
                organisasjoner = listOf(
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Arbeidsgiver 1",
                        skalJobbe = "ja",
                        skalJobbeProsent = 100.0,
                        jobberNormaltTimer = 37.5
                    ),
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Arbeidsgiver 2",
                        skalJobbe = "nei",
                        skalJobbeProsent = 0.0,
                        jobberNormaltTimer = 37.5
                    ),
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Arbeidsgiver 3",
                        skalJobbe = "vetIkke",
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 50.0,
                        vetIkkeEkstrainfo = "Vondt i hode, skulker, kne og tå, kne og tå"
                    ),
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Arbeidsgiver 4",
                        skalJobbe = "redusert",
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 50.0
                    )
                )
            ),
            vedleggUrls = listOf(
                URI("http:localhost:8080/vedlegg1"),
                URI("http:localhost:8080/vedlegg2"),
                URI("http:localhost:8080/vedlegg3")
            ),
            medlemskap = Medlemskap(
                harBoddIUtlandetSiste12Mnd = true,
                utenlandsoppholdSiste12Mnd = listOf(
                    Bosted(
                        LocalDate.of(2020, 1, 2),
                        LocalDate.of(2020, 1, 3),
                        "US", "USA"
                    )
                ),
                skalBoIUtlandetNeste12Mnd = false
            ),
            harMedsøker = true,
            samtidigHjemme = true,
            harForståttRettigheterOgPlikter = true,
            harBekreftetOpplysninger = true,
            tilsynsordning = Tilsynsordning(
                svar = "ja",
                ja = TilsynsordningJa(
                    mandag = Duration.ofHours(0).plusMinutes(0),
                    tirsdag = Duration.ofHours(7).plusMinutes(55),
                    onsdag = null,
                    torsdag = Duration.ofHours(1).plusMinutes(1),
                    fredag = Duration.ofMinutes(43),
                    tilleggsinformasjon = "Unntatt uke 43, da skal han være hos pappaen sin.\rmed\nlinje\r\nlinjeskift."
                ),
                vetIkke = TilsynsordningVetIkke(
                    svar = "annet",
                    annet = "Jeg har ingen anelse om dette\rmed\nlinje\r\nlinjeskift."
                )
            ),
            nattevåk = Nattevåk(
                harNattevåk = true,
                tilleggsinformasjon = "Har nattevåk"
            ),
            beredskap = Beredskap(
                beredskap = true,
                tilleggsinformasjon = "Jeg er i beredskap\rmed\nlinje\r\nlinjeskift."
            ),
            utenlandsoppholdIPerioden = UtenlandsoppholdIPerioden(
                skalOppholdeSegIUtlandetIPerioden = false,
                opphold = listOf()
            ),
            ferieuttakIPerioden = FerieuttakIPerioden(
                skalTaUtFerieIPerioden = true,
                ferieuttak = listOf(
                    Ferieuttak(fraOgMed = LocalDate.parse("2020-01-01"), tilOgMed = LocalDate.parse("2020-01-05")),
                    Ferieuttak(fraOgMed = LocalDate.parse("2020-01-07"), tilOgMed = LocalDate.parse("2020-01-15")),
                    Ferieuttak(fraOgMed = LocalDate.parse("2020-02-01"), tilOgMed = LocalDate.parse("2020-02-05"))
                )
            ),
            frilans = Frilans(
                startdato = LocalDate.now().minusYears(3),
                jobberFortsattSomFrilans = true
            ),
            selvstendigVirksomheter = listOf(
                Virksomhet(
                    næringstyper = listOf(Næringstyper.ANNEN),
                    fraOgMed = LocalDate.now(),
                    tilOgMed = LocalDate.now().plusDays(10),
                    navnPåVirksomheten = "Kjells Møbelsnekkeri",
                    registrertINorge = true,
                    organisasjonsnummer = "111111"
                ),
                Virksomhet(
                    næringstyper = listOf(Næringstyper.JORDBRUK_SKOGBRUK, Næringstyper.DAGMAMMA, Næringstyper.FISKE),
                    fiskerErPåBladB = true,
                    fraOgMed = LocalDate.now(),
                    næringsinntekt = 1111,
                    navnPåVirksomheten = "Tull Og Tøys",
                    registrertINorge = false,
                    registrertIUtlandet = Land(
                        landkode = "DEU",
                        landnavn = "Tyskland"
                    ),
                    yrkesaktivSisteTreFerdigliknedeÅrene = YrkesaktivSisteTreFerdigliknedeÅrene(LocalDate.now()),
                    varigEndring = VarigEndring(
                        dato = LocalDate.now().minusDays(20),
                        inntektEtterEndring = 234543,
                        forklaring = "Forklaring som handler om varig endring"
                    ),
                    regnskapsfører = Regnskapsfører(
                        navn = "Bjarne Regnskap",
                        telefon = "65484578"
                    )
                )
            ),
            bekrefterPeriodeOver8Uker = true,
            skalBekrefteOmsorg = true,
            skalPassePaBarnetIHelePerioden = true,
            beskrivelseOmsorgsrollen = "Jeg er far og skal passe på barnet i hele perioden."
        )
    }

    private fun gyldigMelding(
        soknadId: String,
        sprak: String? = "nb",
        organisasjoner: List<Organisasjon> = listOf(
            Organisasjon(
                organisasjonsnummer = "987564785",
                navn = "NAV",
                jobberNormaltTimer = 30.0,
                skalJobbeProsent = 50.0,
                skalJobbe = "redusert"
            ),
            Organisasjon(
                organisasjonsnummer = "975124568",
                navn = "Kiwi",
                jobberNormaltTimer = 30.0,
                skalJobbeProsent = 50.0,
                skalJobbe = "redusert"
            ),
            Organisasjon(
                organisasjonsnummer = "952352687",
                navn = "Bjerkheim gård",
                jobberNormaltTimer = 30.0,
                skalJobbeProsent = 50.0,
                skalJobbe = "redusert"
            ),
            Organisasjon(
                organisasjonsnummer = "952352655",
                navn = "Hopp i havet",
                jobberNormaltTimer = 30.0,
                skalJobbeProsent = 50.0,
                skalJobbe = "redusert"
            )
        ),
        barn: Barn = Barn(
            navn = "Børge Øverbø Ånsnes",
            fødselsnummer = null,
            fødselsdato = null,
            aktørId = null
        ),
        harMedsøker: Boolean = true,
        samtidigHjemme: Boolean? = false,
        tilsynsordning: Tilsynsordning? = null,
        beredskap: Beredskap? = null,
        nattevaak: Nattevåk? = null,
        medlemskap: Medlemskap = Medlemskap(
            harBoddIUtlandetSiste12Mnd = true,
            utenlandsoppholdSiste12Mnd = listOf(
                Bosted(
                    LocalDate.of(2020, 1, 2),
                    LocalDate.of(2020, 1, 3),
                    "US", "USA"
                )
            ),
            skalBoIUtlandetNeste12Mnd = false
        ),
        frilans: Frilans = Frilans(
            startdato = LocalDate.now().minusYears(3),
            jobberFortsattSomFrilans = true
        ),
        selvstendigVirksomheter: List<Virksomhet>? = listOf(
            Virksomhet(
                næringstyper = listOf(Næringstyper.ANNEN, Næringstyper.FISKE, Næringstyper.JORDBRUK_SKOGBRUK, Næringstyper.DAGMAMMA),
                fiskerErPåBladB = true,
                fraOgMed = LocalDate.now(),
                tilOgMed = LocalDate.now().plusDays(10),
                navnPåVirksomheten = "Kjells Møbelsnekkeriiii",
                registrertINorge = true,
                organisasjonsnummer = "101010",
                varigEndring = VarigEndring(
                    dato = LocalDate.now(),
                    inntektEtterEndring = 202020,
                    forklaring = "ASDASDASDASDASD"
                )
            ),
            Virksomhet(
                næringstyper = listOf(Næringstyper.JORDBRUK_SKOGBRUK, Næringstyper.DAGMAMMA),
                fraOgMed = LocalDate.now(),
                næringsinntekt = 100111,
                navnPåVirksomheten = "Tull Og Tøys",
                registrertINorge = false,
                registrertIUtlandet = Land(
                    landnavn = "Tyskland",
                    landkode = "DEU"
                ),
                yrkesaktivSisteTreFerdigliknedeÅrene = YrkesaktivSisteTreFerdigliknedeÅrene(LocalDate.now()),
                regnskapsfører = Regnskapsfører(
                    navn = "Bjarne Regnskap",
                    telefon = "65484578"
                )
            )
        ),
        vedleggUrls: List<URI> = listOf()
    ) = MeldingV1(
        språk = sprak,
        søknadId = soknadId,
        mottatt = ZonedDateTime.now(),
        vedleggUrls = vedleggUrls,
        fraOgMed = LocalDate.now().plusDays(6),
        tilOgMed = LocalDate.now().plusDays(35),
        søker = Søker(
            aktørId = "123456",
            fornavn = "Ærling",
            mellomnavn = "Øverbø",
            etternavn = "Ånsnes",
            fødselsnummer = "29099012345"
        ),
        barn = barn,
        relasjonTilBarnet = "Onkel & Nærstående ' <> \" {}",
        arbeidsgivere = Arbeidsgivere(
            organisasjoner = organisasjoner
        ),
        medlemskap = medlemskap,
        harMedsøker = harMedsøker,
        bekrefterPeriodeOver8Uker = true,
        samtidigHjemme = samtidigHjemme,
        harForståttRettigheterOgPlikter = true,
        harBekreftetOpplysninger = true,
        tilsynsordning = tilsynsordning,
        nattevåk = nattevaak,
        beredskap = beredskap,
        utenlandsoppholdIPerioden = UtenlandsoppholdIPerioden(
            skalOppholdeSegIUtlandetIPerioden = true,
            opphold = listOf(
                Utenlandsopphold(
                    fraOgMed = LocalDate.parse("2020-01-01"),
                    tilOgMed = LocalDate.parse("2020-01-10"),
                    landnavn = "Bahamas",
                    landkode = "BAH",
                    erUtenforEøs = true,
                    erBarnetInnlagt = true,
                    perioderBarnetErInnlagt = listOf(
                        Periode(
                            fraOgMed = LocalDate.parse("2020-01-01"),
                            tilOgMed = LocalDate.parse("2020-01-01")
                        ),
                        Periode(
                            fraOgMed = LocalDate.parse("2020-01-03"),
                            tilOgMed = LocalDate.parse("2020-01-04")
                        )
                    ),
                    årsak = Årsak.ANNET
                ),
                Utenlandsopphold(
                    fraOgMed = LocalDate.parse("2020-01-01"),
                    tilOgMed = LocalDate.parse("2020-01-10"),
                    landnavn = "Svergie",
                    landkode = "BHS",
                    erUtenforEøs = false,
                    erBarnetInnlagt = true,
                    perioderBarnetErInnlagt = listOf(
                        Periode(
                            fraOgMed = LocalDate.parse("2020-01-01"),
                            tilOgMed = LocalDate.parse("2020-01-01")
                        ),
                        Periode(
                            fraOgMed = LocalDate.parse("2020-01-03"),
                            tilOgMed = LocalDate.parse("2020-01-04")
                        ),
                        Periode(
                            fraOgMed = LocalDate.parse("2020-01-05"),
                            tilOgMed = LocalDate.parse("2020-01-05")
                        )
                    ),
                    årsak = Årsak.ANNET
                )
            )
        ),
        ferieuttakIPerioden = FerieuttakIPerioden(
            skalTaUtFerieIPerioden = true,
            ferieuttak = listOf(
                Ferieuttak(fraOgMed = LocalDate.parse("2020-01-01"), tilOgMed = LocalDate.parse("2020-01-05")),
                Ferieuttak(fraOgMed = LocalDate.parse("2020-01-07"), tilOgMed = LocalDate.parse("2020-01-15")),
                Ferieuttak(fraOgMed = LocalDate.parse("2020-02-01"), tilOgMed = LocalDate.parse("2020-02-05"))
            )
        ),
        frilans = frilans,
        selvstendigVirksomheter = selvstendigVirksomheter
    )

    private fun genererOppsummeringsPdfer(writeBytes: Boolean) {
        var id = "1-full-søknad"
        var pdf = generator.generateSoknadOppsummeringPdf(
            melding = fullGyldigMelding(soknadsId = id),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fødselsdato = fødselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "2-utenMedsoker"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(soknadId = id, harMedsøker = false),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fødselsdato = fødselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "3-medsøkerSamtidigHjemme"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsøker = true,
                samtidigHjemme = true,
                barn = Barn(fødselsnummer = null, fødselsdato = null, navn = null, aktørId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fødselsdato = fødselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "4-medsøkerIkkeSamtidigHjemme"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsøker = true,
                samtidigHjemme = false,
                barn = Barn(fødselsnummer = null, fødselsdato = null, navn = null, aktørId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fødselsdato = fødselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "5-utenSprak"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(soknadId = id, harMedsøker = false, sprak = null),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fødselsdato = fødselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "6-utenArbeidsgivere"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(soknadId = id, harMedsøker = false, organisasjoner = listOf()),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fødselsdato = fødselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "7-utenInfoPaaBarn"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsøker = false,
                organisasjoner = listOf(),
                barn = Barn(fødselsnummer = null, fødselsdato = null, navn = null, aktørId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fødselsdato = fødselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "8-utenGrad"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsøker = false,
                organisasjoner = listOf(),
                barn = Barn(fødselsnummer = null, fødselsdato = null, navn = null, aktørId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fødselsdato = fødselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "9-utenDagerBorteFraJobb"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsøker = false,
                organisasjoner = listOf(),
                barn = Barn(fødselsnummer = null, fødselsdato = null, navn = null, aktørId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fødselsdato = fødselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "10-medTilsynsOrdningJa"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsøker = false,
                barn = Barn(fødselsnummer = null, fødselsdato = null, navn = null, aktørId = null),
                tilsynsordning = Tilsynsordning(
                    svar = "ja",
                    ja = TilsynsordningJa(
                        mandag = Duration.ofHours(0).plusMinutes(0),
                        tirsdag = Duration.ofHours(7).plusMinutes(55),
                        onsdag = null,
                        torsdag = Duration.ofHours(1).plusMinutes(1),
                        fredag = Duration.ofMinutes(43),
                        tilleggsinformasjon = "Unntatt uke 43, da skal han være hos pappaen sin.\rmed\nlinje\r\nlinjeskift."
                    ),
                    vetIkke = null
                ),
                beredskap = Beredskap(
                    beredskap = true,
                    tilleggsinformasjon = "Jeg er i beredskap\rmed\nlinje\r\nlinjeskift."
                ),
                nattevaak = Nattevåk(
                    harNattevåk = false,
                    tilleggsinformasjon = null
                ),
                organisasjoner = listOf(
                    Organisasjon(
                        organisasjonsnummer = "987564785",
                        navn = "NAV",
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 50.0,
                        skalJobbe = "vetIkke"
                    ),
                    Organisasjon(
                        organisasjonsnummer = "975124568",
                        navn = "Kiwi",
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 50.0,
                        skalJobbe = "redusert"
                    )
                )
            ), barnetsIdent = barnetsIdent, barnetsNavn = barnetsNavn,
            fødselsdato = fødselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "11-medTilsynsordningVetIkke"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsøker = false,
                organisasjoner = listOf(),
                barn = Barn(fødselsnummer = null, fødselsdato = null, navn = null, aktørId = null),
                tilsynsordning = Tilsynsordning(
                    svar = "vetIkke",
                    ja = null,
                    vetIkke = TilsynsordningVetIkke(
                        svar = "annet",
                        annet = "Jeg har ingen anelse om dette\rmed\nlinje\r\nlinjeskift."
                    )
                )
            ), barnetsIdent = barnetsIdent, barnetsNavn = barnetsNavn,
            fødselsdato = fødselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)


        id = "12-medTilsynsordningNei"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsøker = false,
                organisasjoner = listOf(),
                barn = Barn(fødselsnummer = null, fødselsdato = null, navn = null, aktørId = null),
                tilsynsordning = Tilsynsordning(
                    svar = "nei",
                    ja = null,
                    vetIkke = null
                )
            ), barnetsIdent = barnetsIdent, barnetsNavn = barnetsNavn,
            fødselsdato = fødselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "13-skalJobbeRedusert"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsøker = true, organisasjoner = listOf(
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Hopp i havet",
                        skalJobbe = "redusert",
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 50.0
                    )
                ), barn = Barn(fødselsnummer = null, fødselsdato = null, navn = null, aktørId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fødselsdato = fødselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "14-skalJobbeVetIkke"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsøker = true, samtidigHjemme = true, organisasjoner = listOf(
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Hopp i havet",
                        skalJobbe = "vetIkke",
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 0.0,
                        vetIkkeEkstrainfo = "Vondt i hode, skulker, kne og tå, kne og tå"
                    )
                ), barn = Barn(fødselsnummer = null, fødselsdato = null, navn = null, aktørId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fødselsdato = fødselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "15-skalJobbeJa"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsøker = true, organisasjoner = listOf(
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Hopp i havet",
                        skalJobbe = "ja",
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 100.0
                    )
                ), barn = Barn(fødselsnummer = null, fødselsdato = null, navn = null, aktørId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fødselsdato = fødselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "16-skalJobbeNei"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsøker = true, organisasjoner = listOf(
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Hopp i havet",
                        skalJobbe = "nei",
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 0.0
                    )
                ), barn = Barn(fødselsnummer = null, fødselsdato = null, navn = null, aktørId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fødselsdato = fødselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "17-flereArbeidsgivereSkalJobbeJaNeiVetIkkeRedusert"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsøker = true, organisasjoner = listOf(
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Arbeidsgiver 1",
                        skalJobbe = "ja",
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 100.0
                    ),
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Arbeidsgiver 2",
                        skalJobbe = "nei",
                        skalJobbeProsent = 0.0,
                        jobberNormaltTimer = 30.0
                    ),
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Arbeidsgiver 3",
                        skalJobbe = "vetIkke",
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 50.0,
                        vetIkkeEkstrainfo = "Vondt i hode, skulker, kne og tå, kne og tå"
                    ),
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Arbeidsgiver 4",
                        skalJobbe = "redusert",
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 50.0
                    )
                ), barn = Barn(fødselsnummer = null, fødselsdato = null, navn = null, aktørId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fødselsdato = fødselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "18-flerePlanlagteUtenlandsopphold"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                medlemskap = Medlemskap(
                    harBoddIUtlandetSiste12Mnd = false,
                    utenlandsoppholdSiste12Mnd = listOf(),
                    skalBoIUtlandetNeste12Mnd = true,
                    utenlandsoppholdNeste12Mnd = listOf(
                        Bosted(
                            LocalDate.of(2022, 1, 2),
                            LocalDate.of(2022, 1, 3),
                            "US", "USA"
                        ), Bosted(
                            LocalDate.of(2022, 1, 3),
                            LocalDate.of(2022, 1, 4),
                            "DK", "Danmark"
                        )
                    )
                )
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fødselsdato = null
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "19-barnHarIkkeIdBareFødselsdato"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsøker = true, organisasjoner = listOf(
                ), barn = Barn(fødselsnummer = null, fødselsdato = LocalDate.now(), navn = null, aktørId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fødselsdato = fødselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)


        id = "20-barnManglerIdOgFødselsdato"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsøker = true, organisasjoner = listOf(
                ), barn = Barn(fødselsnummer = null, fødselsdato = null, navn = null, aktørId = null)
            ),
            barnetsIdent = null,
            barnetsNavn = barnetsNavn,
            fødselsdato = null
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "21-har-du-jobbet-og-hatt-inntekt-som-frilanser"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsøker = true, organisasjoner = listOf(
                ), barn = Barn(fødselsnummer = null, fødselsdato = null, navn = null, aktørId = null)
            ),
            barnetsIdent = null,
            barnetsNavn = barnetsNavn,
            fødselsdato = null
        )

        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "22-har-du-hatt-inntekt-som-selvstendig-næringsdrivende"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsøker = true, organisasjoner = listOf(
                ), barn = Barn(fødselsnummer = null, fødselsdato = null, navn = null, aktørId = null)
            ),
            barnetsIdent = null,
            barnetsNavn = barnetsNavn,
            fødselsdato = null
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)


        id = "23-har-lastet-opp-vedlegg"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsøker = true, organisasjoner = listOf(
                ), barn = Barn(fødselsnummer = null, fødselsdato = null, navn = null, aktørId = null),
                vedleggUrls = listOf(URI("noe"))
            ),
            barnetsIdent = null,
            barnetsNavn = barnetsNavn,
            fødselsdato = null
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

    }

    private fun pdfPath(soknadId: String) = "${System.getProperty("user.dir")}/generated-pdf-$soknadId.pdf"

    @Test
    fun `generering av oppsummerings-PDF fungerer`() {
        genererOppsummeringsPdfer(false)
    }

    @Test
    @Ignore
    fun `opprett lesbar oppsummerings-PDF`() {
        genererOppsummeringsPdfer(true)
    }
}
