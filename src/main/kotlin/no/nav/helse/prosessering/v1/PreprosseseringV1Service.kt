package no.nav.helse.prosessering.v1

import no.nav.helse.CorrelationId
import no.nav.helse.k9mellomlagring.Dokument
import no.nav.helse.k9mellomlagring.DokumentEier
import no.nav.helse.k9mellomlagring.JournalforingsFormat
import no.nav.helse.k9mellomlagring.K9MellomlagringService
import no.nav.helse.pdf.SøknadPDFGenerator
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.SoknadId
import org.slf4j.LoggerFactory
import java.net.URI

internal class PreprosseseringV1Service(
    private val søknadPDFGenerator: SøknadPDFGenerator,
    private val k9MellomlagringService: K9MellomlagringService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(PreprosseseringV1Service::class.java)
    }

    internal suspend fun preprosseser(
        melding: MeldingV1,
        metadata: Metadata
    ): PreprossesertMeldingV1 {
        val soknadId = SoknadId(melding.søknadId)
        logger.info("Preprosseserer $soknadId")

        val correlationId = CorrelationId(metadata.correlationId)
        val dokumentEier = DokumentEier(melding.søker.fødselsnummer)

        logger.info("Genererer Oppsummerings-PDF av søknaden.")
        val oppsummeringPdf = søknadPDFGenerator.genererPDF(melding)

        logger.info("Mellomlagrer Oppsummerings-PDF.")
        val oppsummeringPdDokumentId = k9MellomlagringService.lagreDokument(
            dokument = Dokument(
                eier = dokumentEier,
                content = oppsummeringPdf,
                contentType = "application/pdf",
                title = "Søknad om pleiepenger"
            ),
            correlationId = correlationId
        ).dokumentId()

        logger.info("Mellomlagrer Oppsummerings-JSON")
        val soknadJsonDokumentId = k9MellomlagringService.lagreDokument(
            dokument = Dokument(
                eier = dokumentEier,
                content = JournalforingsFormat.somJson(melding.k9FormatSøknad),
                contentType = "application/json",
                title = "Søknad om pleiepenger som JSON"
            ),
            correlationId = correlationId
        ).dokumentId()

        val komplettDokumentId = mutableListOf(
            listOf(
                oppsummeringPdDokumentId,
                soknadJsonDokumentId
            )
        )

        if (melding.vedleggId.isNotEmpty()) {
            logger.info("Legger til ${melding.vedleggId.size} vedlegg Id's fra meldingen som dokument.")
            melding.vedleggId.forEach { komplettDokumentId.add(listOf(it)) }
        }

        if(!melding.opplastetIdVedleggId.isNullOrEmpty()){
            logger.info("Legger til ${melding.opplastetIdVedleggId.size} opplastetIdVedleggId's fra søknad som dokument.")
            melding.opplastetIdVedleggId.forEach { komplettDokumentId.add(listOf(it)) }
        }

        logger.info("Totalt ${komplettDokumentId.size} dokumentbolker med totalt ${komplettDokumentId.flatten().size} dokumenter")

        val preprossesertMeldingV1 = PreprossesertMeldingV1(
            melding = melding,
            dokumentId = komplettDokumentId.toList()
        )
        melding.reportMetrics()
        preprossesertMeldingV1.reportMetrics()
        return preprossesertMeldingV1
    }
}


fun URI.dokumentId() = this.toString().substringAfterLast("/")
