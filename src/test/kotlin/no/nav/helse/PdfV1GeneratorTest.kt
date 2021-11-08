package no.nav.helse

import no.nav.helse.felles.*
import no.nav.helse.prosessering.v1.ArbeidsforholdAnsatt
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
    }

    private fun fullGyldigMelding(soknadsId: String): MeldingV1 {
        return MeldingV1(
            språk = "nb",
            søknadId = soknadsId,
            mottatt = ZonedDateTime.now(),
            fraOgMed = LocalDate.now().minusDays(6),
            tilOgMed = LocalDate.now().plusDays(35),
            søker = Søker(
                aktørId = "123456",
                fornavn = "Ærling",
                mellomnavn = "ØVERBØ",
                etternavn = "ÅNSNES",
                fødselsnummer = "29099012345"
            ),
            barn = Barn(
                fødselsnummer = "02119970078",
                navn = "OLE DOLE"
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
                        landnavn = "Sverige",
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
                    arbeidsform = Arbeidsform.FAST,
                    jobberNormaltTimer = 23.0,
                    historiskArbeid = ArbeidIPeriode(
                        jobberIPerioden = JobberIPeriodeSvar.JA,
                        jobberSomVanlig = true,
                        erLiktHverUke = null,
                        enkeltdager = listOf(
                            Enkeltdag(dato = LocalDate.now(), tid = Duration.ofHours(1)),
                            Enkeltdag(dato = LocalDate.now().plusDays(3), tid = Duration.ofHours(1)),
                            Enkeltdag(dato = LocalDate.now().plusWeeks(1), tid = Duration.ofHours(1))
                        ),
                        fasteDager = PlanUkedager(
                            mandag = Duration.ofHours(1),
                            tirsdag = Duration.ofHours(1),
                            onsdag = null,
                            torsdag = Duration.ofHours(1).plusMinutes(45),
                            fredag = null
                        )
                    ),
                    planlagtArbeid = ArbeidIPeriode(
                        jobberIPerioden = JobberIPeriodeSvar.JA,
                        jobberSomVanlig = true,
                        erLiktHverUke = true,
                        enkeltdager = listOf(
                            Enkeltdag(dato = LocalDate.now(), tid = Duration.ofHours(1)),
                            Enkeltdag(dato = LocalDate.now().plusDays(3), tid = Duration.ofHours(1)),
                            Enkeltdag(dato = LocalDate.now().plusWeeks(1), tid = Duration.ofHours(1))
                        ),
                        fasteDager = PlanUkedager(
                            mandag = Duration.ofHours(1),
                            tirsdag = Duration.ofHours(1),
                            onsdag = null,
                            torsdag = Duration.ofHours(1).plusMinutes(45),
                            fredag = null
                        )
                    )
                )
            ),
            selvstendigNæringsdrivende = SelvstendigNæringsdrivende(
                virksomhet = Virksomhet(
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
                ),
                arbeidsforhold = Arbeidsforhold(
                    arbeidsform = Arbeidsform.FAST,
                    jobberNormaltTimer = 40.0,
                    historiskArbeid = ArbeidIPeriode(
                        jobberIPerioden = JobberIPeriodeSvar.JA,
                        jobberSomVanlig = true,
                        erLiktHverUke = false,
                        enkeltdager = listOf(
                            Enkeltdag(dato = LocalDate.now(), tid = Duration.ofHours(4)),
                            Enkeltdag(dato = LocalDate.now().plusDays(3), tid = Duration.ofHours(4)),
                            Enkeltdag(dato = LocalDate.now().plusWeeks(1), tid = Duration.ofHours(4))
                        ),
                        fasteDager = PlanUkedager(
                            mandag = Duration.ofHours(4),
                            tirsdag = Duration.ofHours(7),
                            onsdag = null,
                            torsdag = Duration.ofHours(5).plusMinutes(45),
                            fredag = null
                        )
                    ),
                    planlagtArbeid = ArbeidIPeriode(
                        jobberIPerioden = JobberIPeriodeSvar.VET_IKKE,
                        jobberSomVanlig = true,
                        erLiktHverUke = true,
                        enkeltdager = listOf(
                            Enkeltdag(dato = LocalDate.now(), tid = Duration.ofHours(4)),
                            Enkeltdag(dato = LocalDate.now().plusDays(3), tid = Duration.ofHours(4)),
                            Enkeltdag(dato = LocalDate.now().plusWeeks(1), tid = Duration.ofHours(4))
                        ),
                        fasteDager = PlanUkedager(
                            mandag = Duration.ofHours(4),
                            tirsdag = Duration.ofHours(7),
                            onsdag = null,
                            torsdag = Duration.ofHours(5).plusMinutes(45),
                            fredag = null
                        )
                    )
                )
            ),
            arbeidsgivere = listOf(
                ArbeidsforholdAnsatt(
                    navn = "Peppes",
                    organisasjonsnummer = "917755736",
                    erAnsatt = true,
                    arbeidsforhold = Arbeidsforhold(
                        arbeidsform = Arbeidsform.FAST,
                        jobberNormaltTimer = 27.0,
                        historiskArbeid = ArbeidIPeriode(
                            jobberIPerioden = JobberIPeriodeSvar.JA,
                            jobberSomVanlig = true,
                            erLiktHverUke = false,
                            enkeltdager = listOf(
                                Enkeltdag(dato = LocalDate.now(), tid = Duration.ofHours(4)),
                                Enkeltdag(dato = LocalDate.now().plusDays(3), tid = Duration.ofHours(5)),
                                Enkeltdag(dato = LocalDate.now().plusWeeks(1), tid = Duration.ofHours(5))
                            ),
                            fasteDager = PlanUkedager(
                                mandag = Duration.ofHours(4),
                                tirsdag = Duration.ofHours(7),
                                onsdag = null,
                                torsdag = Duration.ofHours(5).plusMinutes(45),
                                fredag = null
                            )
                        ),
                        planlagtArbeid = ArbeidIPeriode(
                            jobberIPerioden = JobberIPeriodeSvar.NEI,
                            jobberSomVanlig = true,
                            erLiktHverUke = true,
                            enkeltdager = listOf(
                                Enkeltdag(dato = LocalDate.now(), tid = Duration.ofHours(5)),
                                Enkeltdag(dato = LocalDate.now().plusDays(3), tid = Duration.ofHours(4)),
                                Enkeltdag(dato = LocalDate.now().plusWeeks(1), tid = Duration.ofHours(5))
                            ),
                            fasteDager = PlanUkedager(
                                mandag = Duration.ofHours(4),
                                tirsdag = Duration.ofHours(7),
                                onsdag = null,
                                torsdag = Duration.ofHours(5).plusMinutes(45),
                                fredag = null
                            )
                        )
                    )
                ),
                ArbeidsforholdAnsatt(
                    navn = "Pizzabakeren",
                    organisasjonsnummer = "917755736",
                    erAnsatt = true,
                    arbeidsforhold = Arbeidsforhold(
                        arbeidsform = Arbeidsform.VARIERENDE,
                        jobberNormaltTimer = 40.0,
                        historiskArbeid = ArbeidIPeriode(
                            jobberIPerioden = JobberIPeriodeSvar.JA,
                            jobberSomVanlig = true,
                            erLiktHverUke = true,
                            enkeltdager = listOf(
                                Enkeltdag(dato = LocalDate.now(), tid = Duration.ofHours(4)),
                                Enkeltdag(dato = LocalDate.now().plusDays(3), tid = Duration.ofHours(9)),
                                Enkeltdag(dato = LocalDate.now().plusWeeks(1), tid = Duration.ofHours(9))
                            ),
                            fasteDager = PlanUkedager(
                                mandag = Duration.ofHours(4),
                                tirsdag = Duration.ofHours(6),
                                onsdag = null,
                                torsdag = Duration.ofHours(5).plusMinutes(45),
                                fredag = null
                            )
                        ),
                        planlagtArbeid = ArbeidIPeriode(
                            jobberIPerioden = JobberIPeriodeSvar.VET_IKKE,
                            jobberSomVanlig = true,
                            erLiktHverUke = true,
                            enkeltdager = null,
                            fasteDager = PlanUkedager(
                                mandag = Duration.ofHours(4),
                                tirsdag = Duration.ofHours(7),
                                onsdag = null,
                                torsdag = Duration.ofHours(5).plusMinutes(45),
                                fredag = null
                            )
                        )
                    )
                ),
                ArbeidsforholdAnsatt(
                    navn = "Sluttaaaa",
                    organisasjonsnummer = "917755736",
                    erAnsatt = false,
                    arbeidsforhold = null
                )
            ),
            harVærtEllerErVernepliktig = true,
            barnRelasjon = BarnRelasjon.ANNET,
            barnRelasjonBeskrivelse = "Blaabla annet",
            k9FormatSøknad = SøknadUtils.defaultK9FormatPSB(), omsorgstilbud = null
        )
    }

    private fun genererOppsummeringsPdfer(writeBytes: Boolean) {
        var id = "1-full-søknad"
        var pdf = generator.generateSoknadOppsummeringPdf(
            melding = fullGyldigMelding(soknadsId = id)
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "2-utenMedsøker"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = fullGyldigMelding(id).copy(harMedsøker = false)
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "3-medsøkerSamtidigHjemme"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = fullGyldigMelding(id).copy(
                harMedsøker = true,
                samtidigHjemme = true
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "4-medsøkerIkkeSamtidigHjemme"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = fullGyldigMelding(id).copy(
                harMedsøker = true,
                samtidigHjemme = false
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "5-utenSpråk"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = fullGyldigMelding(id).copy(harMedsøker = false, språk = null)
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "6-utenArbeidsgivere"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = fullGyldigMelding(id).copy(arbeidsgivere = null)
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "7-flerePlanlagteUtenlandsopphold"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = fullGyldigMelding(id).copy(
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
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "8-har-lastet-opp-vedlegg"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = fullGyldigMelding(id).copy(
                harMedsøker = true,
                vedleggUrls = listOf(URI("noe"))
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "9-omsorgstilbud-v2-med-historisk-og-planlagte-ukedager"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = fullGyldigMelding(id).copy(
                fraOgMed = LocalDate.now().minusDays(10),
                tilOgMed = LocalDate.now().plusDays(10),
                omsorgstilbud = Omsorgstilbud(
                    historisk = HistoriskOmsorgstilbud(
                        enkeltdager = listOf(
                            Enkeltdag(LocalDate.now().minusDays(3), Duration.ofHours(7).plusMinutes(30)),
                            Enkeltdag(LocalDate.now().minusDays(2), Duration.ofHours(7).plusMinutes(30)),
                            Enkeltdag(LocalDate.now().minusDays(1), Duration.ofHours(7).plusMinutes(30)),
                        )
                    ),
                    planlagt = PlanlagtOmsorgstilbud(
                        ukedager = PlanUkedager(
                            mandag = null,
                            tirsdag = Duration.ofHours(7).plusMinutes(30),
                            onsdag = null,
                            torsdag = Duration.ofHours(7).plusMinutes(30),
                            fredag = Duration.ofHours(7).plusMinutes(30),
                        )
                    )
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)


        id = "10-omsorgstilbud-v2-med-historisk-og-planlagte-enkeltdager"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = fullGyldigMelding(id).copy(
                fraOgMed = LocalDate.now().minusDays(10),
                tilOgMed = LocalDate.now().plusDays(10),
                omsorgstilbud = Omsorgstilbud(
                    historisk = HistoriskOmsorgstilbud(
                        enkeltdager = listOf(
                            Enkeltdag(LocalDate.parse("2021-01-01"), Duration.ofHours(7).plusMinutes(30)),
                            Enkeltdag(LocalDate.now().minusDays(3), Duration.ofHours(7).plusMinutes(30)),
                            Enkeltdag(LocalDate.now().minusDays(2), Duration.ofHours(7).plusMinutes(30)),
                            Enkeltdag(LocalDate.now().minusDays(1), Duration.ofHours(7).plusMinutes(30))
                        )
                    ),
                    planlagt = PlanlagtOmsorgstilbud(
                        enkeltdager = listOf(
                            Enkeltdag(LocalDate.now().plusDays(1), Duration.ofHours(7).plusMinutes(30)),
                            Enkeltdag(LocalDate.now().plusDays(2), Duration.ofHours(7).plusMinutes(30)),
                            Enkeltdag(LocalDate.now().plusDays(3), Duration.ofHours(7).plusMinutes(30)),
                            Enkeltdag(LocalDate.now().plusDays(4), Duration.ofHours(0))
                        ),
                        erLiktHverDag = false
                    )
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "11-omsorgstilbud-nei-til-omsorgstilbud"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = fullGyldigMelding(id).copy(
                omsorgstilbud = null
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "12-kun-frilans-arbeidsforhold"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = fullGyldigMelding(id).copy(
                selvstendigNæringsdrivende = null,
                arbeidsgivere = null
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
