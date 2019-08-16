package no.nav.helse

import no.nav.helse.prosessering.v1.*
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.Ignore
import kotlin.test.Test

class PdfV1GeneratorTest {

    private companion object {
        private val generator = PdfV1Generator()
    }

    private fun gyldigMelding(
        soknadId: String,
        sprak: String? = "nb",
        organisasjoner: List<Organisasjon> = listOf(
            Organisasjon(
                organisasjonsnummer = "987564785",
                navn = "NAV"
            ),
            Organisasjon(
                organisasjonsnummer = "975124568",
                navn = "Kiwi",
                normalArbeidsuke = Duration.ofHours(37).plusMinutes(1),
                redusertArbeidsuke = Duration.ofHours(10).plusMinutes(45)
            ),
            Organisasjon(
                organisasjonsnummer = "952352687",
                navn = "Bjerkheim gård",
                normalArbeidsuke = Duration.ofHours(15).plusMinutes(50),
                redusertArbeidsuke = Duration.ZERO
            ),
            Organisasjon(
                organisasjonsnummer = "952352655",
                navn = "Hopp i havet",
                normalArbeidsuke = Duration.ofHours(100),
                redusertArbeidsuke = Duration.ofHours(81)
            )
        ),
        barn: Barn = Barn(
            navn = "Børge Øverbø Ånsnes",
            fodselsnummer = null,
            alternativId = "29091884321"
        ),
        grad: Int? = 60,
        harMedsoker: Boolean = true
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
        medlemskap = Medlemskap(
            harBoddIUtlandetSiste12Mnd = true,
            skalBoIUtlandetNeste12Mnd = false
        ),
        grad = grad,
        harMedsoker = harMedsoker,
        harForstattRettigheterOgPlikter = true,
        harBekreftetOpplysninger = true
    )

    private fun genererOppsummeringsPdfer(writeBytes: Boolean) {
        var id = "1-full"
        var pdf = generator.generateSoknadOppsummeringPdf(melding = gyldigMelding(soknadId = id))
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "2-utenMedsoker"
        pdf = generator.generateSoknadOppsummeringPdf(melding = gyldigMelding(soknadId = id, harMedsoker = false))
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "3-utenSprak"
        pdf = generator.generateSoknadOppsummeringPdf(melding = gyldigMelding(soknadId = id, harMedsoker = false, sprak = null))
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "4-utenArbeidsgivere"
        pdf = generator.generateSoknadOppsummeringPdf(melding = gyldigMelding(soknadId = id, harMedsoker = false, organisasjoner = listOf()))
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "5-utenInfoPaaBarn"
        pdf = generator.generateSoknadOppsummeringPdf(melding = gyldigMelding(soknadId = id, harMedsoker = false, organisasjoner = listOf(), barn = Barn(fodselsnummer = null, alternativId = null, navn = null)))
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "6-utenGrad"
        pdf = generator.generateSoknadOppsummeringPdf(melding = gyldigMelding(soknadId = id, harMedsoker = false, organisasjoner = listOf(), barn = Barn(fodselsnummer = null, alternativId = null, navn = null), grad = null))
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