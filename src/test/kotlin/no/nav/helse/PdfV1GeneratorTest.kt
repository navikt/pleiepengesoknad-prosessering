package no.nav.helse

import no.nav.helse.felles.*
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.PdfV1Generator
import java.io.File
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.Test

class PdfV1GeneratorTest {

    private companion object {
        private val generator = PdfV1Generator()
        private val barnetsIdent = "02119970078"
        private val barnetsFødselsdato = LocalDate.parse("1999-11-02")
        private val barnetsNavn = "OLE DOLE"
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
                mellomnavn = "ØVERBØ",
                etternavn = "ÅNSNES",
                fødselsnummer = "29099012345"
            ),
            barn = Barn(
                fødselsnummer = barnetsIdent,
                navn = barnetsNavn
            ),
            arbeidsgivere = Arbeidsgivere(
                organisasjoner = listOf(
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Arbeidsgiver 1",
                        skalJobbe = SkalJobbe.JA,
                        skalJobbeProsent = 100.0,
                        jobberNormaltTimer = 37.5,
                        arbeidsform = Arbeidsform.FAST
                    ),
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Arbeidsgiver 2",
                        skalJobbe = SkalJobbe.NEI,
                        skalJobbeProsent = 0.0,
                        jobberNormaltTimer = 37.5,
                        arbeidsform = Arbeidsform.VARIERENDE
                    ),
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Arbeidsgiver 3",
                        skalJobbe = SkalJobbe.VET_IKKE,
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 50.0,
                        vetIkkeEkstrainfo = "Vondt i hode, skulker, kne og tå, kne og tå",
                        arbeidsform = Arbeidsform.FAST
                    ),
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Arbeidsgiver 4",
                        skalJobbe = SkalJobbe.REDUSERT,
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 50.0,
                        arbeidsform = Arbeidsform.TURNUS
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
            nattevåk = Nattevåk(
                harNattevåk = true,
                tilleggsinformasjon = "Har nattevåk"
            ),
            beredskap = Beredskap(
                beredskap = true,
                tilleggsinformasjon = "Jeg er i beredskap\rmed\nlinje\r\nlinjeskift."
            ),
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
            frilans = Frilans(
                startdato = LocalDate.now().minusYears(3),
                sluttdato = LocalDate.now(),
                jobberFortsattSomFrilans = false,
                arbeidsforhold = Arbeidsforhold(
                    skalJobbe = SkalJobbe.NEI,
                    arbeidsform = Arbeidsform.FAST,
                    jobberNormaltTimer = 40.0,
                    skalJobbeProsent = 0.0
                )
            ),
            selvstendigVirksomheter = listOf(
                Virksomhet(
                    næringstyper = listOf(Næringstyper.ANNEN),
                    fraOgMed = LocalDate.now(),
                    tilOgMed = LocalDate.now().plusDays(10),
                    navnPåVirksomheten = "Kjells Møbelsnekkeri",
                    registrertINorge = true,
                    organisasjonsnummer = "111111",
                    harFlereAktiveVirksomheter = true
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
                    ),
                    harFlereAktiveVirksomheter = true
                )
            ),
            selvstendigArbeidsforhold = Arbeidsforhold(
                skalJobbe = SkalJobbe.REDUSERT,
                arbeidsform = Arbeidsform.VARIERENDE,
                jobberNormaltTimer = 40.0,
                skalJobbeProsent = 50.0
            ),
            skalBekrefteOmsorg = true,
            skalPassePaBarnetIHelePerioden = true,
            beskrivelseOmsorgsrollen = "Jeg er far og skal passe på barnet i hele perioden.",
            harVærtEllerErVernepliktig = true,
            barnRelasjon = BarnRelasjon.ANNET,
            barnRelasjonBeskrivelse = "Blaabla annet",
            k9FormatSøknad = SøknadUtils.defaultK9FormatPSB()
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
                skalJobbe = SkalJobbe.REDUSERT,
                arbeidsform = Arbeidsform.FAST
            ),
            Organisasjon(
                organisasjonsnummer = "975124568",
                navn = "Kiwi",
                jobberNormaltTimer = 30.0,
                skalJobbeProsent = 50.0,
                skalJobbe = SkalJobbe.REDUSERT,
                arbeidsform = Arbeidsform.VARIERENDE
            ),
            Organisasjon(
                organisasjonsnummer = "952352687",
                navn = "Bjerkheim gård",
                jobberNormaltTimer = 30.0,
                skalJobbeProsent = 50.0,
                skalJobbe = SkalJobbe.REDUSERT,
                arbeidsform = Arbeidsform.TURNUS
            ),
            Organisasjon(
                organisasjonsnummer = "952352655",
                navn = "Hopp i havet",
                jobberNormaltTimer = 30.0,
                skalJobbeProsent = 50.0,
                skalJobbe = SkalJobbe.REDUSERT,
                arbeidsform = Arbeidsform.FAST
            )
        ),
        barn: Barn = Barn(
            navn = "Børge Øverbø Ånsnes",
            fødselsnummer = barnetsIdent
        ),
        harMedsøker: Boolean = true,
        samtidigHjemme: Boolean? = false,
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
            sluttdato = LocalDate.now(),
            jobberFortsattSomFrilans = false
        ),
        selvstendigVirksomheter: List<Virksomhet> = listOf(
            Virksomhet(
                næringstyper = listOf(
                    Næringstyper.ANNEN,
                    Næringstyper.FISKE,
                    Næringstyper.JORDBRUK_SKOGBRUK,
                    Næringstyper.DAGMAMMA
                ),
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
        arbeidsgivere = Arbeidsgivere(
            organisasjoner = organisasjoner
        ),
        medlemskap = medlemskap,
        harMedsøker = harMedsøker,
        samtidigHjemme = samtidigHjemme,
        harForståttRettigheterOgPlikter = true,
        harBekreftetOpplysninger = true,
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
        selvstendigVirksomheter = selvstendigVirksomheter,
        barnRelasjon = BarnRelasjon.FAR,
        barnRelasjonBeskrivelse = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec iaculis tempus molestie",
        harVærtEllerErVernepliktig = true,
        k9FormatSøknad = SøknadUtils.defaultK9FormatPSB()
    )

    private fun genererOppsummeringsPdfer(writeBytes: Boolean) {
        var id = "1-full-søknad"
        var pdf = generator.generateSoknadOppsummeringPdf(
            melding = fullGyldigMelding(soknadsId = id)
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "2-utenMedsoker"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(soknadId = id, harMedsøker = false)
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "3-medsøkerSamtidigHjemme"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsøker = true,
                samtidigHjemme = true
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "4-medsøkerIkkeSamtidigHjemme"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsøker = true,
                samtidigHjemme = false
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "5-utenSprak"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(soknadId = id, harMedsøker = false, sprak = null)
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "6-utenArbeidsgivere"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(soknadId = id, harMedsøker = false, organisasjoner = listOf())
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "7-utenGrad"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsøker = false,
                organisasjoner = listOf()
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "8-utenDagerBorteFraJobb"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsøker = false,
                organisasjoner = listOf()
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "9-skalJobbeRedusert"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsøker = true, organisasjoner = listOf(
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Hopp i havet",
                        skalJobbe = SkalJobbe.REDUSERT,
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 50.0,
                        arbeidsform = Arbeidsform.VARIERENDE
                    )
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "10-skalJobbeVetIkke"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsøker = true, samtidigHjemme = true, organisasjoner = listOf(
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Hopp i havet",
                        skalJobbe = SkalJobbe.VET_IKKE,
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 0.0,
                        vetIkkeEkstrainfo = "Vondt i hode, skulker, kne og tå, kne og tå",
                        arbeidsform = Arbeidsform.VARIERENDE
                    )
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "11-skalJobbeJa"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsøker = true, organisasjoner = listOf(
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Hopp i havet",
                        skalJobbe = SkalJobbe.JA,
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 100.0,
                        arbeidsform = Arbeidsform.VARIERENDE
                    )
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "12-skalJobbeNei"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsøker = true, organisasjoner = listOf(
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Hopp i havet",
                        skalJobbe = SkalJobbe.NEI,
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 0.0,
                        arbeidsform = Arbeidsform.VARIERENDE
                    )
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "13-flereArbeidsgivereSkalJobbeJaNeiVetIkkeRedusert"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsøker = true, organisasjoner = listOf(
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Arbeidsgiver 1",
                        skalJobbe = SkalJobbe.JA,
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 100.0,
                        arbeidsform = Arbeidsform.VARIERENDE
                    ),
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Arbeidsgiver 2",
                        skalJobbe = SkalJobbe.NEI,
                        skalJobbeProsent = 0.0,
                        jobberNormaltTimer = 30.0,
                        arbeidsform = Arbeidsform.VARIERENDE
                    ),
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Arbeidsgiver 3",
                        skalJobbe = SkalJobbe.VET_IKKE,
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 50.0,
                        vetIkkeEkstrainfo = "Vondt i hode, skulker, kne og tå, kne og tå",
                        arbeidsform = Arbeidsform.VARIERENDE
                    ),
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Arbeidsgiver 4",
                        skalJobbe = SkalJobbe.REDUSERT,
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 50.0,
                        arbeidsform = Arbeidsform.VARIERENDE
                    )
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "14-flerePlanlagteUtenlandsopphold"
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

            )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "15-barnHarIkkeIdBareFødselsdato"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsøker = true, organisasjoner = listOf(
                )
            ),
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)


        id = "16-barnManglerIdOgFødselsdato"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsøker = true, organisasjoner = listOf()
            ),
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "17-har-du-jobbet-og-hatt-inntekt-som-frilanser"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsøker = true, organisasjoner = listOf()
            )
        )

        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "18-har-du-hatt-inntekt-som-selvstendig-næringsdrivende"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsøker = true, organisasjoner = listOf()
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "19-har-lastet-opp-vedlegg"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsøker = true, organisasjoner = listOf(),
                vedleggUrls = listOf(URI("noe"))
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "20-omsorgstilbud-med-usikker-tilsyn-vet-min-antall-timer"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = SøknadUtils.defaultSøknad.copy(
                omsorgstilbud = Omsorgstilbud(
                    fasteDager = OmsorgstilbudUkedager(
                        mandag = Duration.ofHours(2).plusMinutes(30),
                        tirsdag = Duration.ofHours(2).plusMinutes(30),
                        onsdag = Duration.ofHours(2).plusMinutes(30),
                        torsdag = Duration.ofHours(2).plusMinutes(30),
                        fredag = Duration.ofHours(2).plusMinutes(30),
                    ),
                    vetOmsorgstilbud = VetOmsorgstilbud.VET_NOEN_TIMER
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "21-omsorgstilbud-med-usikker-tilsyn-vet-ikke-min-antall-timer"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = SøknadUtils.defaultSøknad.copy(
                omsorgstilbud = Omsorgstilbud(
                    fasteDager = null,
                    vetOmsorgstilbud = VetOmsorgstilbud.VET_IKKE
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "22-uten-omsorgstilbud"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = SøknadUtils.defaultSøknad.copy(
                omsorgstilbud = null
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "23-omsorgstilbud-med-tilsyn"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = SøknadUtils.defaultSøknad.copy(
                omsorgstilbud = Omsorgstilbud(
                    fasteDager = OmsorgstilbudUkedager(
                        mandag = Duration.ofHours(7).plusMinutes(30),
                        tirsdag = Duration.ofHours(7),
                        onsdag = null,
                        torsdag = Duration.ofHours(4).plusMinutes(30),
                        fredag = Duration.ofHours(7).plusMinutes(30),
                    ),
                    vetOmsorgstilbud = VetOmsorgstilbud.VET_ALLE_TIMER
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)


        id = "24-omsorgstilbud-med-usikker-tilsyn-vet-min-antall-timer"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = SøknadUtils.defaultSøknad.copy(
                omsorgstilbud = Omsorgstilbud(
                    fasteDager = OmsorgstilbudUkedager(
                        mandag = Duration.ofHours(2).plusMinutes(30),
                        tirsdag = Duration.ofHours(2).plusMinutes(30),
                        onsdag = Duration.ofHours(2).plusMinutes(30),
                        torsdag = Duration.ofHours(2).plusMinutes(30),
                        fredag = Duration.ofHours(2).plusMinutes(30),
                    ),
                    vetOmsorgstilbud = VetOmsorgstilbud.VET_NOEN_TIMER
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)


        id = "25-omsorgstilbud-med-usikker-tilsyn-vet-ikke-min-antall-timer"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = SøknadUtils.defaultSøknad.copy(
                omsorgstilbud = Omsorgstilbud(
                    fasteDager = null,
                    vetOmsorgstilbud = VetOmsorgstilbud.VET_IKKE
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "26-omsorgstilbud-v2-med-historisk-og-planlagte-ukedager"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = SøknadUtils.defaultSøknad.copy(
                omsorgstilbud = null,
                omsorgstilbudV2 = OmsorgstilbudV2(
                    historisk = HistoriskOmsorgstilbud(
                        enkeltdager = listOf(
                            Omsorgsdag(LocalDate.now().minusDays(3), Duration.ofHours(7).plusMinutes(30)),
                            Omsorgsdag(LocalDate.now().minusDays(2), Duration.ofHours(7).plusMinutes(30)),
                            Omsorgsdag(LocalDate.now().minusDays(1), Duration.ofHours(7).plusMinutes(30)),
                        )
                    ),
                    planlagt = PlanlagtOmsorgstilbud(
                        ukedager = OmsorgstilbudUkedager(
                            mandag = Duration.ofHours(7).plusMinutes(30),
                            tirsdag = Duration.ofHours(7).plusMinutes(30),
                            torsdag = Duration.ofHours(7).plusMinutes(30),
                            fredag = Duration.ofHours(7).plusMinutes(30),
                        ),
                        vetOmsorgstilbud = VetOmsorgstilbud.VET_ALLE_TIMER
                    )
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)


        id = "27-oomsorgstilbud-v2-med-historisk-og-planlagte-enkeltdager"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = SøknadUtils.defaultSøknad.copy(
                fraOgMed = LocalDate.parse("2021-01-01"),
                tilOgMed = LocalDate.parse("2021-12-01"),
                omsorgstilbudV2 = OmsorgstilbudV2(
                    historisk = HistoriskOmsorgstilbud(
                        enkeltdager = listOf(
                            Omsorgsdag(LocalDate.parse("2021-01-01"), Duration.ofHours(7).plusMinutes(30)),
                            Omsorgsdag(LocalDate.now().minusDays(3), Duration.ofHours(7).plusMinutes(30)),
                            Omsorgsdag(LocalDate.now().minusDays(2), Duration.ofHours(7).plusMinutes(30)),
                            Omsorgsdag(LocalDate.now().minusDays(1), Duration.ofHours(7).plusMinutes(30))
                        )
                    ),
                    planlagt = PlanlagtOmsorgstilbud(
                        enkeltdager = listOf(
                            Omsorgsdag(LocalDate.now().plusDays(1), Duration.ofHours(7).plusMinutes(30)),
                            Omsorgsdag(LocalDate.now().plusDays(2), Duration.ofHours(7).plusMinutes(30)),
                            Omsorgsdag(LocalDate.now().plusDays(3), Duration.ofHours(7).plusMinutes(30)),
                            Omsorgsdag(LocalDate.now().plusDays(4), Duration.ofHours(0))
                        ),
                        vetOmsorgstilbud = VetOmsorgstilbud.VET_ALLE_TIMER,
                        erLiktHverDag = false
                    )
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "28-omsorgstilbud-v2-vet-ikke-planlagt-tilsyn"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = SøknadUtils.defaultSøknad.copy(
                omsorgstilbud = null,
                omsorgstilbudV2 = OmsorgstilbudV2(
                    planlagt = PlanlagtOmsorgstilbud(
                        vetOmsorgstilbud = VetOmsorgstilbud.VET_IKKE
                    )
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)
    }

    private fun pdfPath(soknadId: String) = "${System.getProperty("user.dir")}/generated-pdf-$soknadId.pdf"

    @Test
    fun `generering av oppsummerings-PDF fungerer`() {
        genererOppsummeringsPdfer(false)
    }

    @Test
    //@Ignore
    fun `opprett lesbar oppsummerings-PDF`() {
        genererOppsummeringsPdfer(true)
    }
}
