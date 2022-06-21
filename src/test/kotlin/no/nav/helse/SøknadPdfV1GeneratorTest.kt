package no.nav.helse

import no.nav.helse.felles.*
import no.nav.helse.pdf.SøknadPDFGenerator
import no.nav.helse.prosessering.v1.Arbeidsgiver
import no.nav.helse.prosessering.v1.MeldingV1
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.Test

class SøknadPdfV1GeneratorTest {

    private companion object {
        private val generator = SøknadPDFGenerator()
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
                fødselsnummer = "02119970078",
                navn = "OLE DOLE"
            ),
            vedleggId = listOf("123", "456"),
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
            utenlandskNæring = listOf(
                UtenlandskNæring(
                    næringstype = Næringstyper.FISKE,
                    navnPåVirksomheten = "Fiskeriet AS",
                    land = Land(landkode = "NDL", landnavn = "Nederland"),
                    identifikasjonsnummer = "123ABC",
                    fraOgMed = LocalDate.parse("2020-01-09")
                )
            ),
            frilans = Frilans(
                harInntektSomFrilanser = true,
                startdato = LocalDate.now().minusYears(3),
                sluttdato = LocalDate.now(),
                jobberFortsattSomFrilans = false,
                arbeidsforhold = Arbeidsforhold(
                    normalarbeidstid = NormalArbeidstid(
                        erLiktHverUke = true,
                        timerFasteDager = PlanUkedager(
                            mandag = Duration.ofHours(3),
                            onsdag = Duration.ofHours(3),
                            fredag = Duration.ofHours(3)
                        ),
                    ),
                    arbeidIPeriode = ArbeidIPeriode(
                        type = ArbeidIPeriodeType.ARBEIDER_VANLIG,
                        arbeiderIPerioden = ArbeiderIPeriodenSvar.SOM_VANLIG
                    )
                )
            ),
            selvstendigNæringsdrivende = SelvstendigNæringsdrivende(
                harInntektSomSelvstendig = true,
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
                    normalarbeidstid = NormalArbeidstid(
                        erLiktHverUke = true,
                        timerPerUkeISnitt = Duration.ofHours(37).plusMinutes(30)
                    ),
                    arbeidIPeriode = ArbeidIPeriode(
                        type = ArbeidIPeriodeType.ARBEIDER_VANLIG,
                        arbeiderIPerioden = ArbeiderIPeriodenSvar.SOM_VANLIG
                    )
                )
            ),
            arbeidsgivere = listOf(
                Arbeidsgiver(
                    navn = "Peppes",
                    organisasjonsnummer = "917755736",
                    erAnsatt = true,
                    arbeidsforhold = Arbeidsforhold(
                        normalarbeidstid = NormalArbeidstid(
                            erLiktHverUke = true,
                            timerFasteDager = PlanUkedager(
                                mandag = Duration.ofHours(3),
                                onsdag = Duration.ofHours(3),
                                fredag = Duration.ofHours(3)
                            )
                        ),
                        arbeidIPeriode = ArbeidIPeriode(
                            type = ArbeidIPeriodeType.ARBEIDER_VANLIG,
                            arbeiderIPerioden = ArbeiderIPeriodenSvar.SOM_VANLIG
                        )
                    )
                ),
                Arbeidsgiver(
                    navn = "Pizzabakeren",
                    organisasjonsnummer = "917755736",
                    erAnsatt = true,
                    arbeidsforhold = Arbeidsforhold(
                        normalarbeidstid = NormalArbeidstid(
                            erLiktHverUke = true,
                            timerPerUkeISnitt = Duration.ofHours(37).plusMinutes(30)
                        ),
                        arbeidIPeriode = ArbeidIPeriode(
                            type = ArbeidIPeriodeType.ARBEIDER_FASTE_UKEDAGER,
                            arbeiderIPerioden = ArbeiderIPeriodenSvar.REDUSERT,
                            fasteDager = PlanUkedager(
                                mandag = Duration.ofHours(7),
                                onsdag = Duration.ofHours(8),
                                fredag = Duration.ofHours(5)
                            )
                        )
                    )
                ),
                Arbeidsgiver(
                    navn = "Sluttaaaa",
                    organisasjonsnummer = "917755736",
                    erAnsatt = false,
                    arbeidsforhold = null,
                    sluttetFørSøknadsperiode = true
                )
            ),
            harVærtEllerErVernepliktig = true,
            barnRelasjon = BarnRelasjon.ANNET,
            barnRelasjonBeskrivelse = "Blaabla annet",
            k9FormatSøknad = SøknadUtils.defaultK9FormatPSB(),
            omsorgstilbud = Omsorgstilbud(
                erLiktHverUke = true,
                ukedager = PlanUkedager(
                    mandag = Duration.ofHours(3),
                    onsdag = Duration.ofHours(3),
                    fredag = Duration.ofHours(3)
                ),
                enkeltdager = listOf(
                    Enkeltdag(LocalDate.now(), Duration.ofHours(3)),
                    Enkeltdag(LocalDate.now().plusDays(3), Duration.ofHours(2)),
                    Enkeltdag(LocalDate.now().plusWeeks(4), Duration.ofHours(4)),
                    Enkeltdag(LocalDate.now().plusWeeks(4), Duration.ofHours(6).plusMinutes(45)),
                    Enkeltdag(LocalDate.now().plusWeeks(9).plusDays(2), Duration.ofHours(3))
                )
            )
        )
    }

    private fun genererOppsummeringsPdfer(writeBytes: Boolean) {
        var id = "1-full-søknad"
        var pdf = generator.genererPDF(
            melding = fullGyldigMelding(soknadsId = id)
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "2-utenMedsøker"
        pdf = generator.genererPDF(
            melding = fullGyldigMelding(id).copy(harMedsøker = false)
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "3-medsøkerSamtidigHjemme"
        pdf = generator.genererPDF(
            melding = fullGyldigMelding(id).copy(
                harMedsøker = true,
                samtidigHjemme = true
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "4-medsøkerIkkeSamtidigHjemme"
        pdf = generator.genererPDF(
            melding = fullGyldigMelding(id).copy(
                harMedsøker = true,
                samtidigHjemme = false
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "5-utenSpråk"
        pdf = generator.genererPDF(
            melding = fullGyldigMelding(id).copy(harMedsøker = false, språk = null)
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "6-utenArbeidsgivere"
        pdf = generator.genererPDF(
            melding = fullGyldigMelding(id).copy(arbeidsgivere = listOf())
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "7-flerePlanlagteUtenlandsopphold"
        pdf = generator.genererPDF(
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
        pdf = generator.genererPDF(
            melding = fullGyldigMelding(id).copy(
                harMedsøker = true,
                vedleggId = listOf("12345")
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "9-omsorgstilbud-nei-til-omsorgstilbud"
        pdf = generator.genererPDF(
            melding = fullGyldigMelding(id).copy(
                omsorgstilbud = null
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "10-omsorgstilbud-omsorgstilbud-enkeltdager"
        pdf = generator.genererPDF(
            melding = fullGyldigMelding(id).copy(
                omsorgstilbud = Omsorgstilbud(
                    erLiktHverUke = false,
                    enkeltdager = listOf(
                        Enkeltdag(LocalDate.now(), Duration.ofHours(3)),
                        Enkeltdag(LocalDate.now().plusDays(3), Duration.ofHours(2)),
                        Enkeltdag(LocalDate.now().plusWeeks(4), Duration.ofHours(4)),
                        Enkeltdag(LocalDate.now().plusWeeks(4), Duration.ofHours(6).plusMinutes(45)),
                        Enkeltdag(LocalDate.now().plusWeeks(9).plusDays(2), Duration.ofHours(3))
                    )
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "11-omsorgstilbud-omsorgstilbud-ukedager"
        pdf = generator.genererPDF(
            melding = fullGyldigMelding(id).copy(
                omsorgstilbud = Omsorgstilbud(
                    erLiktHverUke = true,
                    ukedager = PlanUkedager(
                        mandag = Duration.ofHours(3),
                        onsdag = Duration.ofHours(3),
                        fredag = Duration.ofHours(3)
                    ),
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "12-kun-frilans-arbeidsforhold"
        pdf = generator.genererPDF(
            melding = fullGyldigMelding(id).copy(
                selvstendigNæringsdrivende = SelvstendigNæringsdrivende(harInntektSomSelvstendig = false),
                arbeidsgivere = listOf()
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "13-barn-med-årsakManglerIdentitetsnummer"
        pdf = generator.genererPDF(
            melding = fullGyldigMelding(id).copy(
                barn = Barn(
                    navn = "OLE DOLE",
                    fødselsdato = LocalDate.now(),
                    årsakManglerIdentitetsnummer = ÅrsakManglerIdentitetsnummer.NYFØDT
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "14-med-opptjening-i-utlandet"
        pdf = generator.genererPDF(
            melding = fullGyldigMelding(id).copy(
                opptjeningIUtlandet = listOf(
                    OpptjeningIUtlandet(
                        navn = "Kiwi AS",
                        opptjeningType = OpptjeningType.ARBEIDSTAKER,
                        land = Land(
                            landkode = "IKKE GYLDIG",
                            landnavn = "Belgia",
                        ),
                        fraOgMed = LocalDate.parse("2022-01-01"),
                        tilOgMed = LocalDate.parse("2022-01-10")
                    )
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "15-med-utenlandsk-næring"
        pdf = generator.genererPDF(
            melding = fullGyldigMelding(id).copy(
                selvstendigNæringsdrivende = SelvstendigNæringsdrivende(false),
                frilans = Frilans(false),
                arbeidsgivere = listOf(),
                utenlandskNæring = listOf(
                    UtenlandskNæring(
                        næringstype = Næringstyper.FISKE,
                        navnPåVirksomheten = "Fiskeriet AS",
                        land = Land(landkode = "NDL", landnavn = "Nederland"),
                        identifikasjonsnummer = "123ABC",
                        fraOgMed = LocalDate.parse("2020-01-09")
                    ),
                    UtenlandskNæring(
                        næringstype = Næringstyper.DAGMAMMA,
                        navnPåVirksomheten = "Dagmamma AS",
                        land = Land(landkode = "NDL", landnavn = "Nederland"),
                        identifikasjonsnummer = "123ABC",
                        fraOgMed = LocalDate.parse("2020-01-09"),
                        tilOgMed = LocalDate.parse("2022-01-09")
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
