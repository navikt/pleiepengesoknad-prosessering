package no.nav.helse

import no.nav.helse.felles.Søker
import no.nav.helse.pdf.EndringsmeldingPDFGenerator
import no.nav.helse.prosessering.v1.asynkron.endringsmelding.EndringsmeldingV1
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Organisasjonsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.felles.type.SøknadId
import no.nav.k9.søknad.ytelse.psb.v1.DataBruktTilUtledning
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstaker
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidInfo
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidPeriodeInfo
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.Test

class EndringsmeldingPdfV1GeneratorTest {

    private companion object {
        private val generator = EndringsmeldingPDFGenerator()
    }

    private fun fullGyldigEndringsmelding(søknadsId: String): EndringsmeldingV1 {
        fun k9FormatEndringsmelding(søknadId: UUID = UUID.randomUUID()) = Søknad(
            SøknadId.of(søknadId.toString()),
            Versjon.of("1.0.0"),
            ZonedDateTime.parse("2020-01-01T10:00:00Z"),
            no.nav.k9.søknad.felles.personopplysninger.Søker(NorskIdentitetsnummer.of("12345678910")),
            PleiepengerSyktBarn()
                .medSøknadsperiode(Periode(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-10")))
                .medSøknadInfo(DataBruktTilUtledning(true, true, true, true, true))
                .medBarn(Barn().medNorskIdentitetsnummer(NorskIdentitetsnummer.of("10987654321")))
                .medArbeidstid(
                    Arbeidstid()
                        .medArbeidstaker(
                            listOf(
                                Arbeidstaker()
                                    .medNorskIdentitetsnummer(NorskIdentitetsnummer.of("12345678910"))
                                    .medOrganisasjonsnummer(Organisasjonsnummer.of("926032925"))
                                    .medArbeidstidInfo(
                                        ArbeidstidInfo()
                                            .medPerioder(
                                                mapOf(
                                                    Periode(
                                                        LocalDate.parse("2022-12-26"),
                                                        LocalDate.parse("2022-12-30")
                                                    ) to ArbeidstidPeriodeInfo()
                                                        .medJobberNormaltTimerPerDag(Duration.ofHours(8))
                                                        .medFaktiskArbeidTimerPerDag(Duration.ofHours(4)),
                                                    Periode(
                                                        LocalDate.parse("2023-01-02"),
                                                        LocalDate.parse("2023-01-06")
                                                    ) to ArbeidstidPeriodeInfo()
                                                        .medJobberNormaltTimerPerDag(Duration.ofHours(8))
                                                        .medFaktiskArbeidTimerPerDag(Duration.ofHours(2)),
                                                    Periode(
                                                        LocalDate.parse("2023-01-23"),
                                                        LocalDate.parse("2023-01-27")
                                                    ) to ArbeidstidPeriodeInfo()
                                                        .medJobberNormaltTimerPerDag(Duration.ofHours(8))
                                                        .medFaktiskArbeidTimerPerDag(Duration.ofHours(2))
                                                )
                                            )
                                    )
                            )
                        )
                        .medFrilanserArbeidstid(
                            ArbeidstidInfo()
                                .medPerioder(
                                    mapOf(
                                        Periode(
                                            LocalDate.parse("2022-12-26"),
                                            LocalDate.parse("2022-12-30")
                                        ) to ArbeidstidPeriodeInfo()
                                            .medJobberNormaltTimerPerDag(Duration.ofHours(8))
                                            .medFaktiskArbeidTimerPerDag(Duration.ofHours(4)),
                                        Periode(
                                            LocalDate.parse("2023-01-02"),
                                            LocalDate.parse("2023-01-06")
                                        ) to ArbeidstidPeriodeInfo()
                                            .medJobberNormaltTimerPerDag(Duration.ofHours(8))
                                            .medFaktiskArbeidTimerPerDag(Duration.ofHours(2)),
                                        Periode(
                                            LocalDate.parse("2023-01-23"),
                                            LocalDate.parse("2023-01-27")
                                        ) to ArbeidstidPeriodeInfo()
                                            .medJobberNormaltTimerPerDag(Duration.ofHours(8))
                                            .medFaktiskArbeidTimerPerDag(Duration.ofHours(2))
                                    )
                                )
                        )
                        .medSelvstendigNæringsdrivendeArbeidstidInfo(
                            ArbeidstidInfo()
                                .medPerioder(
                                    mapOf(
                                        Periode(
                                            LocalDate.parse("2022-12-26"),
                                            LocalDate.parse("2022-12-30")
                                        ) to ArbeidstidPeriodeInfo()
                                            .medJobberNormaltTimerPerDag(Duration.ofHours(8))
                                            .medFaktiskArbeidTimerPerDag(Duration.ofHours(4)),
                                        Periode(
                                            LocalDate.parse("2023-01-02"),
                                            LocalDate.parse("2023-01-06")
                                        ) to ArbeidstidPeriodeInfo()
                                            .medJobberNormaltTimerPerDag(Duration.ofHours(8))
                                            .medFaktiskArbeidTimerPerDag(Duration.ofHours(2)),
                                        Periode(
                                            LocalDate.parse("2023-01-23"),
                                            LocalDate.parse("2023-01-27")
                                        ) to ArbeidstidPeriodeInfo()
                                            .medJobberNormaltTimerPerDag(Duration.ofHours(8))
                                            .medFaktiskArbeidTimerPerDag(Duration.ofHours(2))
                                    )
                                )
                        )
                )
        )
        return EndringsmeldingV1(
            søker = Søker(
                aktørId = "123456",
                fornavn = "Ærling",
                mellomnavn = "ØVERBØ",
                etternavn = "ÅNSNES",
                fødselsnummer = "29099012345"
            ),
            harBekreftetOpplysninger = true,
            harForståttRettigheterOgPlikter = true,
            k9Format = k9FormatEndringsmelding(søknadId = UUID.fromString(søknadsId))
        )
    }

    private fun genererOppsummeringsPdfer(writeBytes: Boolean) {
        var id = "1-full-endringsmelding"
        var pdf = generator.genererPDF(
            melding = fullGyldigEndringsmelding(søknadsId = UUID.randomUUID().toString())
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)
    }

    private fun pdfPath(soknadId: String) =
        "${System.getProperty("user.dir")}/generated-endringsmelding-pdf-$soknadId.pdf"

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
