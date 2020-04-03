package no.nav.helse

import no.nav.helse.aktoer.Fodselsnummer
import no.nav.helse.prosessering.v1.*
import no.nav.helse.prosessering.v1.ettersending.Ettersending
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
        private val fodselsdato = LocalDate.now().minusDays(10)
    }

    private fun fullGyldigMelding(soknadsId: String): MeldingV1 {
        return MeldingV1(
            sprak = "nb",
            soknadId = soknadsId,
            mottatt = ZonedDateTime.now(),
            fraOgMed = LocalDate.now().plusDays(6),
            tilOgMed = LocalDate.now().plusDays(35),
            soker = Soker(
                aktoerId = "123456",
                fornavn = "Ærling",
                mellomnavn = "Øverbø",
                etternavn = "Ånsnes",
                fodselsnummer = "29099012345"
            ),
            barn = Barn(
                fodselsnummer = barnetsIdent.getValue(),
                aktoerId = "123456",
                navn = barnetsNavn,
                fodselsdato = null
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
                        skalJobbe = "vet_ikke",
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
            harMedsoker = true,
            samtidigHjemme = true,
            harForstattRettigheterOgPlikter = true,
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
            nattevaak = Nattevaak(
                harNattevaak = true,
                tilleggsinformasjon = "Har nattevåk"
            ),
            beredskap = Beredskap(
                beredskap = true,
                tilleggsinformasjon = "Jeg er i beredskap\rmed\nlinje\r\nlinjeskift."
            ),
            utenlandsoppholdIPerioden = null,
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
                    naringstype = listOf(Naringstype.ANNET),
                    fraOgMed = LocalDate.now(),
                    tilOgMed = LocalDate.now().plusDays(10),
                    navnPaVirksomheten = "Kjells Møbelsnekkeri",
                    registrertINorge = true,
                    organisasjonsnummer = "111111",
                    revisor = Revisor(
                        navn = "Kjell Revisor",
                        telefon = "9999",
                        kanInnhenteOpplysninger = true
                    )
                ),
                Virksomhet(
                    naringstype = listOf(Naringstype.JORDBRUK, Naringstype.DAGMAMMA, Naringstype.FISKER),
                    fiskerErPåBladB = true,
                    fraOgMed = LocalDate.now(),
                    naringsinntekt = 1111,
                    navnPaVirksomheten = "Tull Og Tøys",
                    registrertINorge = false,
                    registrertILand = "Bahamas",
                    yrkesaktivSisteTreFerdigliknedeArene = YrkesaktivSisteTreFerdigliknedeArene(LocalDate.now()),
                    varigEndring = VarigEndring(
                        dato = LocalDate.now().minusDays(20),
                        inntektEtterEndring = 234543,
                        forklaring = "Forklaring som handler om varig endring"
                    ),
                    regnskapsforer = Regnskapsforer(
                        navn = "Bjarne Regnskap",
                        telefon = "65484578"
                    )
                )
            ),
            bekrefterPeriodeOver8Uker = true,
            skalBekrefteOmsorg = true,
            skalPassePaBarnetIHelePerioden = true,
            beskrivelseOmsorgsRollen = "Jeg er far og skal passe på barnet i hele perioden."
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
            fodselsnummer = null,
            fodselsdato = null,
            aktoerId = null
        ),
        harMedsoker: Boolean = true,
        samtidigHjemme: Boolean? = false,
        tilsynsordning: Tilsynsordning? = null,
        beredskap: Beredskap? = null,
        nattevaak: Nattevaak? = null,
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
                naringstype = listOf(Naringstype.ANNET, Naringstype.FISKER),
                fiskerErPåBladB = true,
                fraOgMed = LocalDate.now(),
                tilOgMed = LocalDate.now().plusDays(10),
                navnPaVirksomheten = "Kjells Møbelsnekkeri",
                registrertINorge = true,
                organisasjonsnummer = "101010",
                varigEndring = VarigEndring(
                    dato = LocalDate.now(),
                    inntektEtterEndring = 202020,
                    forklaring = "ASDASDASDASDASD"
                ),
                revisor = Revisor(
                    navn = "Kjell Revisor",
                    telefon = "9999",
                    kanInnhenteOpplysninger = true
                )
            ),
            Virksomhet(
                naringstype = listOf(Naringstype.JORDBRUK, Naringstype.DAGMAMMA),
                fraOgMed = LocalDate.now(),
                naringsinntekt = 100111,
                navnPaVirksomheten = "Tull Og Tøys",
                registrertINorge = false,
                registrertILand = "Bahamas",
                yrkesaktivSisteTreFerdigliknedeArene = YrkesaktivSisteTreFerdigliknedeArene(LocalDate.now()),
                regnskapsforer = Regnskapsforer(
                    navn = "Bjarne Regnskap",
                    telefon = "65484578"
                )
            )
        )
    ) = MeldingV1(
        sprak = sprak,
        soknadId = soknadId,
        mottatt = ZonedDateTime.now(),
        fraOgMed = LocalDate.now().plusDays(6),
        tilOgMed = LocalDate.now().plusDays(35),
        soker = Soker(
            aktoerId = "123456",
            fornavn = "Ærling",
            mellomnavn = "Øverbø",
            etternavn = "Ånsnes",
            fodselsnummer = "29099012345"
        ),
        barn = barn,
        relasjonTilBarnet = "Onkel & Nærstående ' <> \" {}",
        arbeidsgivere = Arbeidsgivere(
            organisasjoner = organisasjoner
        ),
        medlemskap = medlemskap,
        harMedsoker = harMedsoker,
        bekrefterPeriodeOver8Uker = true,
        samtidigHjemme = samtidigHjemme,
        harForstattRettigheterOgPlikter = true,
        harBekreftetOpplysninger = true,
        tilsynsordning = tilsynsordning,
        nattevaak = nattevaak,
        beredskap = beredskap,
        utenlandsoppholdIPerioden = null,
        ferieuttakIPerioden = null,
        frilans = frilans,
        selvstendigVirksomheter = selvstendigVirksomheter
    )

    private fun gyldigEttersending() = Ettersending(
        sprak = "nb",
        mottatt = ZonedDateTime.now(),
        harBekreftetOpplysninger = true,
        harForstattRettigheterOgPlikter = true,
        soknadId = "Ettersending",
        soker = Soker(
            aktoerId = "123456",
            fornavn = "Ærling",
            mellomnavn = "Øverbø",
            etternavn = "Ånsnes",
            fodselsnummer = "29099012345"
        ),
        beskrivelse = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Sed accumsan erat cursus enim aliquet, ac auctor orci consequat. " +
                "Etiam nec tellus sapien. Nam gravida massa id sagittis ultrices.",
        soknadstype = "Omsorgspenger",
        vedleggUrls = listOf(
            URI("http://localhost:8081/vedlegg1"),
            URI("http://localhost:8081/vedlegg2"),
            URI("http://localhost:8081/vedlegg3")
        ),
        titler = listOf("vedlegg1","vedlegg2", "vedlegg3")
    )

    private fun genererOppsummeringsPdfer(writeBytes: Boolean) {
        var id = "1-full-søknad"
        var pdf = generator.generateSoknadOppsummeringPdf(
            melding = fullGyldigMelding(soknadsId = id),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fodselsdato = fodselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "2-utenMedsoker"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(soknadId = id, harMedsoker = false),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fodselsdato = fodselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "3-medsøkerSamtidigHjemme"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsoker = true,
                samtidigHjemme = true,
                barn = Barn(fodselsnummer = null, fodselsdato = null, navn = null, aktoerId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fodselsdato = fodselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "4-medsøkerIkkeSamtidigHjemme"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsoker = true,
                samtidigHjemme = false,
                barn = Barn(fodselsnummer = null, fodselsdato = null, navn = null, aktoerId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fodselsdato = fodselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "5-utenSprak"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(soknadId = id, harMedsoker = false, sprak = null),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fodselsdato = fodselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "6-utenArbeidsgivere"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(soknadId = id, harMedsoker = false, organisasjoner = listOf()),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fodselsdato = fodselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "7-utenInfoPaaBarn"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsoker = false,
                organisasjoner = listOf(),
                barn = Barn(fodselsnummer = null, fodselsdato = null, navn = null, aktoerId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fodselsdato = fodselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "8-utenGrad"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsoker = false,
                organisasjoner = listOf(),
                barn = Barn(fodselsnummer = null, fodselsdato = null, navn = null, aktoerId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fodselsdato = fodselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "9-utenDagerBorteFraJobb"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsoker = false,
                organisasjoner = listOf(),
                barn = Barn(fodselsnummer = null, fodselsdato = null, navn = null, aktoerId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fodselsdato = fodselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "10-medTilsynsOrdningJa"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsoker = false,
                barn = Barn(fodselsnummer = null, fodselsdato = null, navn = null, aktoerId = null),
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
                nattevaak = Nattevaak(
                    harNattevaak = false,
                    tilleggsinformasjon = null
                ),
                organisasjoner = listOf(
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
                    )
                )
            ), barnetsIdent = barnetsIdent, barnetsNavn = barnetsNavn,
            fodselsdato = fodselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "11-medTilsynsordningVetIkke"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsoker = false,
                organisasjoner = listOf(),
                barn = Barn(fodselsnummer = null, fodselsdato = null, navn = null, aktoerId = null),
                tilsynsordning = Tilsynsordning(
                    svar = "vet_ikke",
                    ja = null,
                    vetIkke = TilsynsordningVetIkke(
                        svar = "annet",
                        annet = "Jeg har ingen anelse om dette\rmed\nlinje\r\nlinjeskift."
                    )
                )
            ), barnetsIdent = barnetsIdent, barnetsNavn = barnetsNavn,
            fodselsdato = fodselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)


        id = "12-medTilsynsordningNei"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsoker = false,
                organisasjoner = listOf(),
                barn = Barn(fodselsnummer = null, fodselsdato = null, navn = null, aktoerId = null),
                tilsynsordning = Tilsynsordning(
                    svar = "nei",
                    ja = null,
                    vetIkke = null
                )
            ), barnetsIdent = barnetsIdent, barnetsNavn = barnetsNavn,
            fodselsdato = fodselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "13-skalJobbeRedusert"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsoker = true, organisasjoner = listOf(
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Hopp i havet",
                        skalJobbe = "redusert",
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 50.0
                    )
                ), barn = Barn(fodselsnummer = null, fodselsdato = null, navn = null, aktoerId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fodselsdato = fodselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "14-skalJobbeVetIkke"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsoker = true, samtidigHjemme = true, organisasjoner = listOf(
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Hopp i havet",
                        skalJobbe = "vet_ikke",
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 0.0,
                        vetIkkeEkstrainfo = "Vondt i hode, skulker, kne og tå, kne og tå"
                    )
                ), barn = Barn(fodselsnummer = null, fodselsdato = null, navn = null, aktoerId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fodselsdato = fodselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "15-skalJobbeJa"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsoker = true, organisasjoner = listOf(
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Hopp i havet",
                        skalJobbe = "ja",
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 100.0
                    )
                ), barn = Barn(fodselsnummer = null, fodselsdato = null, navn = null, aktoerId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fodselsdato = fodselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "16-skalJobbeNei"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsoker = true, organisasjoner = listOf(
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Hopp i havet",
                        skalJobbe = "nei",
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 0.0
                    )
                ), barn = Barn(fodselsnummer = null, fodselsdato = null, navn = null, aktoerId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fodselsdato = fodselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "17-flereArbeidsgivereSkalJobbeJaNeiVetIkkeRedusert"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsoker = true, organisasjoner = listOf(
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
                        skalJobbe = "vet_ikke",
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
                ), barn = Barn(fodselsnummer = null, fodselsdato = null, navn = null, aktoerId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fodselsdato = fodselsdato
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
            fodselsdato = null
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "19-barnHarIkkeIdBareFødselsdato"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsoker = true, organisasjoner = listOf(
                ), barn = Barn(fodselsnummer = null, fodselsdato = LocalDate.now(), navn = null, aktoerId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn,
            fodselsdato = fodselsdato
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)


        id = "20-barnManglerIdOgFødselsdato"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsoker = true, organisasjoner = listOf(
                ), barn = Barn(fodselsnummer = null, fodselsdato = null, navn = null, aktoerId = null)
            ),
            barnetsIdent = null,
            barnetsNavn = barnetsNavn,
            fodselsdato = null
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "21-har-du-jobbet-og-hatt-inntekt-som-frilanser"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsoker = true, organisasjoner = listOf(
                ), barn = Barn(fodselsnummer = null, fodselsdato = null, navn = null, aktoerId = null)
            ),
            barnetsIdent = null,
            barnetsNavn = barnetsNavn,
            fodselsdato = null
        )

        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "22-har-du-hatt-inntekt-som-selvstendig-næringsdrivende"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsoker = true, organisasjoner = listOf(
                ), barn = Barn(fodselsnummer = null, fodselsdato = null, navn = null, aktoerId = null)
            ),
            barnetsIdent = null,
            barnetsNavn = barnetsNavn,
            fodselsdato = null
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "23-komplett-ettersending"
        pdf = generator.generateSoknadOppsummeringPdfEttersending(
            ettersending = gyldigEttersending()
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
