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

    private fun gyldigMelding(soknadId: String) = MeldingV1(
        soknadId = soknadId,
        mottatt = ZonedDateTime.now(),
        fraOgMed = LocalDate.now().plusDays(6),
        tilOgMed = LocalDate.now().plusDays(16),
        soker = Soker(
            aktoerId = "123456",
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
        relasjonTilBarnet = "Onkel & Nærstående ' <> \" {}",
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
        ),
        grad = 60,
        harMedsoker = true,
        harForstattRettigheterOgPlikter = true,
        harBekreftetOpplysninger = true
    )

    private fun genererPdf(soknadId: String = UUID.randomUUID().toString(), melding: MeldingV1 = gyldigMelding(soknadId)) : ByteArray {
        return PdfV1Generator().generateSoknadOppsummeringPdf(melding)
    }

    @Test
    fun `generering av oppsummerings-PDF fungerer`() {
        genererPdf()
    }

    @Test
    @Ignore
    fun `opprett lesbar oppsummerings-PDF`() {
        val soknadId = UUID.randomUUID().toString()
        val path = "${System.getProperty("user.dir")}/generated-pdf-$soknadId.pdf"
        logger.info("Legger generert fil på $path")
        val file = File(path)
        file.writeBytes(genererPdf(soknadId = soknadId))
    }
}