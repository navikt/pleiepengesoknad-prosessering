package no.nav.helse.prosessering.v1.asynkron.endringsmelding

import no.nav.helse.CorrelationId
import no.nav.helse.dokument.DokumentService
import no.nav.helse.pdf.EndringsmeldingPDFGenerator
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.SoknadId
import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.Søknad
import org.slf4j.LoggerFactory

internal class EndringsmeldingPreprosseseringV1Service(
    private val endringsmeldingPDFGenerator: EndringsmeldingPDFGenerator,
    private val dokumentService: DokumentService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(EndringsmeldingPreprosseseringV1Service::class.java)
    }

    internal suspend fun preprosseser(
        endringsmelding: EndringsmeldingV1,
        metadata: Metadata
    ): PreprossesertEndringsmeldingV1 {
        val k9Format = JsonUtils.fromString(endringsmelding.k9Format, Søknad::class.java)
        val soknadId = SoknadId(k9Format.søknadId.id)
        logger.info("Preprosseserer endringsmelding med søknadId $soknadId")

        val correlationId = CorrelationId(metadata.correlationId)

        val søkerAktørId = endringsmelding.søker.aktørId

        logger.trace("Genererer Oppsummerings-PDF av endringsmelding.")

        val endringsmeldingOppsummeringPdf = endringsmeldingPDFGenerator.genererPDF(endringsmelding)

        logger.trace("Generering av Oppsummerings-PDF for endringsmelding OK.")
        logger.trace("Mellomlagrer Oppsummerings-PDF av endringsmelding.")

        val endringsmeldingOppsummeringPdfUrl = dokumentService.lagrePdf(
            pdf = endringsmeldingOppsummeringPdf,
            correlationId = correlationId,
            aktørId = søkerAktørId,
            dokumentbeskrivelse = "Endringsmelding om pleiepenger"
        )

        logger.trace("Mellomlagring av Oppsummerings-PDF for endringsmelding OK")

        logger.trace("Mellomlagrer Oppsummerings-JSON for endringsmelding")

        val endringsmeldingJsonUrl = dokumentService.lagreJsonMelding(
            k9FormatSøknad = k9Format,
            aktørId = søkerAktørId,
            correlationId = correlationId
        )

        logger.trace("Mellomlagring av oppsummerings-JSON for endringsmelding OK.")


        val komplettDokumentUrls = mutableListOf(
            listOf(
                endringsmeldingOppsummeringPdfUrl,
                endringsmeldingJsonUrl
            )
        )

        logger.trace("Totalt ${komplettDokumentUrls.size} dokumentbolker.")

        return PreprossesertEndringsmeldingV1(
            endringsmelding = endringsmelding,
            k9Format = k9Format,
            dokumentUrls = komplettDokumentUrls.toList()
        )
    }
}
