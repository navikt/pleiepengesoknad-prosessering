package no.nav.helse.prosessering.v1

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerService
import no.nav.helse.aktoer.Fodselsnummer
import no.nav.helse.dokument.DokumentGateway
import no.nav.helse.gosys.GosysService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("nav.ProsesseringV1Service")

class ProsesseringV1Service(
    private val gosysService: GosysService,
    private val aktoerService: AktoerService,
    private val pdfV1Generator: PdfV1Generator,
    private val dokumentGateway: DokumentGateway
) {
    suspend fun prosesser(
        melding: MeldingV1,
        metadata: MetadataV1
    ) {
        logger.info(metadata.toString())

        // TODO: Validere melding

        val correlationId = CorrelationId(metadata.correlationId)

        logger.trace("Henter AktørID for søkeren.")
        val sokerAktoerId = aktoerService.getAktorId(
            fnr = Fodselsnummer(melding.soker.fodselsnummer),
            correlationId = correlationId
        )

        logger.trace("Sokers AktørID = $sokerAktoerId")

        logger.trace("Henter AktørID for barnet.")
        val barnAktoerId = if (melding.barn.fodselsnummer != null) {
            aktoerService.getAktorId(
                fnr = Fodselsnummer(melding.barn.fodselsnummer),
                correlationId = correlationId
            )
        } else null

        logger.trace("Barnets AktørID = $barnAktoerId")

        logger.trace("Genererer Oppsummerings-PDF av søknaden.")

        val soknadOppsummeringPdf = pdfV1Generator.generateSoknadOppsummeringPdf(melding)

        logger.trace("Generering av PDF OK. Laster opp PDF for mellomlagring.")

        val soknadOppsummeringUrl = dokumentGateway.lagrePdf(
            pdf = soknadOppsummeringPdf,
            aktoerId = sokerAktoerId,
            correlationId = correlationId,
            tittel = "Søknad om pleiepeinger"
        )

        logger.trace("Mellomlagring OK, Oppretter oppgave i Gosys")

        val dokumenter = mutableListOf(soknadOppsummeringUrl)
        dokumenter.addAll(melding.vedlegg)

        gosysService.opprett(
            sokerAktoerId = sokerAktoerId,
            barnAktoerId = barnAktoerId,
            mottatt = melding.mottatt,
            dokumenter = dokumenter.toList(),
            correlationId = correlationId
        )

        logger.trace("Oppgave i Gosys opprettet OK")
    }
}