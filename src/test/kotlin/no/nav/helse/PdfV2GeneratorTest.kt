package no.nav.helse

import no.nav.helse.SøknadUtils.defaultMeldingV2
import no.nav.helse.prosessering.v2.PdfV2Generator.Companion.generateSoknadOppsummeringPdf
import java.io.File
import kotlin.test.Test

class PdfV2GeneratorTest {

    private fun genererOppsummeringsPdfer(writeBytes: Boolean) {
        var id = "1-full-søknad"
        var pdf = defaultMeldingV2.generateSoknadOppsummeringPdf()
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

    }

    private fun pdfPath(soknadId: String) = "${System.getProperty("user.dir")}/generated-pdf-v2-$soknadId.pdf"

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
