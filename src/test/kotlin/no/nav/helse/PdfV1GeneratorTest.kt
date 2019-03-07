package no.nav.helse

import no.nav.helse.prosessering.v1.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.Ignore
import kotlin.test.Test

private val logger: Logger = LoggerFactory.getLogger("nav.PdfV1GeneratorTest")

class PdfV1GeneratorTest {

    private val gyldigMelding = MeldingV1(
        mottatt = ZonedDateTime.now(),
        fraOgMed = LocalDate.now(),
        tilOgMed = LocalDate.now(),
        soker = Soker(
            fornavn = "Ærling",
            mellomnavn = "Øverbø",
            etternavn = "Ånsnes",
            fodselsnummer = "29099012345"
        ),
        barn = Barn(
            navn = "Børge Øverbø Ånsnes",
            fodselsnummer = null,
            alternativId = "29091812345"
        ),
        relasjonTilBarnet = "Far",
        arbeidsgivere = Arbeidsgivere(
            organisasjoner = listOf(
                Organisasjon(
                    organisasjonsnummer = "1231456",
                    navn = "NAV"
                ),
                Organisasjon(
                    organisasjonsnummer = "1231457",
                    navn = "KIWI"
                )
            )
        ),
        medlemskap = Medlemskap(
            harBoddIUtlandetSiste12Mnd = true,
            skalBoIUtlandetNeste12Mnd = false
        )
    )

    private fun genererPdf(melding: MeldingV1 = gyldigMelding) : ByteArray {
        return PdfV1Generator().generateSoknadOppsummeringPdf(melding)
    }

    @Test
    fun `generering av oppsummerings-PDF fungerer`() {
        genererPdf()
    }

    @Test
    @Ignore
    fun `opprett lesbar oppsummerings-PDF`() {
        val path = "${System.getProperty("user.dir")}/generated-pdf-${UUID.randomUUID()}.pdf"
        logger.info("Legger generert fil på $path")
        val file = File(path)
        file.writeBytes(genererPdf())
    }
}