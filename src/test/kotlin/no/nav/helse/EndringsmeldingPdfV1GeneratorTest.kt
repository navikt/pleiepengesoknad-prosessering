package no.nav.helse

import no.nav.helse.felles.Søker
import no.nav.helse.pdf.EndringsmeldingPDFGenerator
import no.nav.helse.prosessering.v1.asynkron.endringsmelding.EndringsmeldingV1
import java.io.File
import kotlin.test.Test

class EndringsmeldingPdfV1GeneratorTest {

    private companion object {
        private val generator = EndringsmeldingPDFGenerator()
    }

    private fun fullGyldigEndringsmelding(soknadsId: String): EndringsmeldingV1 {
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
            k9Format = SøknadUtils.defaultK9FormatPSB()
        )
    }

    private fun genererOppsummeringsPdfer(writeBytes: Boolean) {
        var id = "1-full-endringsmelding"
        var pdf = generator.genererPDF(
            melding = fullGyldigEndringsmelding(soknadsId = id)
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
