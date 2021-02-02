package no.nav.helse

import no.nav.helse.SøknadUtils.defaultK9FormatPSB
import no.nav.helse.aktoer.Fodselsnummer
import no.nav.helse.felles.*
import no.nav.helse.prosessering.v1.*
import no.nav.helse.prosessering.v2.PdfV2Generator
import no.nav.helse.prosessering.v2.PdfV2Generator.Companion.generateSoknadOppsummeringPdf
import org.junit.Ignore
import java.io.File
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.Test

class PdfV2GeneratorTest {

    private fun genererOppsummeringsPdfer(writeBytes: Boolean) {
        var id = "1-full-søknad"
        var pdf = defaultK9FormatPSB.generateSoknadOppsummeringPdf()
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
