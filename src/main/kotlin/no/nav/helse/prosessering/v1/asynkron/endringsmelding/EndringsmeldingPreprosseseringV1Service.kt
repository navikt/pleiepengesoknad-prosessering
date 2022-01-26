package no.nav.helse.prosessering.v1.asynkron.endringsmelding

import no.nav.helse.CorrelationId
import no.nav.helse.k9mellomlagring.Dokument
import no.nav.helse.k9mellomlagring.DokumentEier
import no.nav.helse.k9mellomlagring.JournalforingsFormat
import no.nav.helse.k9mellomlagring.K9MellomlagringService
import no.nav.helse.pdf.EndringsmeldingPDFGenerator
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.v1.dokumentId
import org.slf4j.LoggerFactory

internal class EndringsmeldingPreprosseseringV1Service(
    private val endringsmeldingPDFGenerator: EndringsmeldingPDFGenerator,
    private val k9MellomlagringService: K9MellomlagringService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(EndringsmeldingPreprosseseringV1Service::class.java)
    }

    internal suspend fun preprosesser(
        endringsmelding: EndringsmeldingV1,
        metadata: Metadata
    ): PreprossesertEndringsmeldingV1 {
        logger.info("Preprosesserer endringsmelding med søknadId ${endringsmelding.k9Format.søknadId}")

        val k9Format = endringsmelding.k9Format
        val correlationId = CorrelationId(metadata.correlationId)
        val dokumentEier = DokumentEier(endringsmelding.søker.fødselsnummer)

        logger.info("Genererer Oppsummerings-PDF av endringsmelding.")
        val endringsmeldingOppsummeringPdf = endringsmeldingPDFGenerator.genererPDF(endringsmelding)

        logger.info("Mellomlagrer Oppsummerings-PDF av endringsmelding.")
        val endringsmeldingOppsummeringPdfDokumentId: String = k9MellomlagringService.lagreDokument(
            dokument = Dokument(
                eier = dokumentEier,
                content = endringsmeldingOppsummeringPdf,
                contentType = "application/pdf",
                title = "Endringsmelding om pleiepenger"
            ),
            correlationId = correlationId
        ).dokumentId()

        logger.info("Mellomlagrer Oppsummerings-JSON for endringsmelding")
        val endringsmeldingJsonDokumentId: String = k9MellomlagringService.lagreDokument(
            dokument = Dokument(
                eier = dokumentEier,
                content = JournalforingsFormat.somJson(k9Format),
                contentType = "application/json",
                title = "Endringsmelding om pleiepenger som JSON"
            ),
            correlationId = correlationId
        ).dokumentId()

        val komplettDokumentIds = mutableListOf(
            listOf(
                endringsmeldingOppsummeringPdfDokumentId,
                endringsmeldingJsonDokumentId
            )
        )

        logger.trace("Totalt ${komplettDokumentIds.size} dokumentbolker.")

        return PreprossesertEndringsmeldingV1(
            endringsmelding = endringsmelding,
            k9Format = k9Format,
            dokumentId = komplettDokumentIds.toList()
        )
    }
}
