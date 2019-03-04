package no.nav.helse.prosessering.v1

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerService
import no.nav.helse.aktoer.Fodselsnummer
import no.nav.helse.dokument.DokumentService
import no.nav.helse.gosys.GosysService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("nav.ProsesseringV1Service")

class ProsesseringV1Service(
    private val gosysService: GosysService,
    private val aktoerService: AktoerService,
    private val pdfV1Generator: PdfV1Generator,
    private val dokumentService: DokumentService
) {
    suspend fun leggSoknadTilProsessering(
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

        logger.trace("Søkerens AktørID = $sokerAktoerId")

        logger.trace("Henter AktørID for barnet.")
        val barnAktoerId = if (melding.barn.fodselsnummer != null) {
            aktoerService.getAktorId(
                fnr = Fodselsnummer(melding.barn.fodselsnummer),
                correlationId = correlationId
            )
        } else null // TODO: Håndter feil her som null

        logger.trace("Barnets AktørID = $barnAktoerId")

        logger.trace("Genererer Oppsummerings-PDF av søknaden.")

        val soknadOppsummeringPdf = pdfV1Generator.generateSoknadOppsummeringPdf(melding)

        logger.trace("Generering av Oppsummerings-PDF OK. Laster opp PDF for mellomlagring.")

        val soknadOppsummeringUrl = dokumentService.lagreSoknadsOppsummeringPdf(
            pdf = soknadOppsummeringPdf,
            correlationId = correlationId
        )

        logger.trace("Mellomlagring av Oppsummerings-PDF OK")

        val komplettDokumentUrls = mutableListOf(soknadOppsummeringUrl)
        if (melding.vedleggUrls.isNotEmpty()) {
            logger.trace("Legger til ${melding.vedleggUrls.size} vedlegg URL's fra meldingen som dokument.")
            komplettDokumentUrls.addAll(melding.vedleggUrls)
        }
        if (melding.vedlegg.isNotEmpty()) {
            logger.trace("Meldingen inneholder ${melding.vedlegg.size} vedlegg som må mellomlagres før søknaden legges til prosessering.")
            val lagredeVedleggUrls = dokumentService.lagreVedlegg(
                vedlegg = melding.vedlegg,
                correlationId = correlationId
            )
            logger.trace("Mellomlagring OK, legger til URL's som dokument.")
            komplettDokumentUrls.addAll(lagredeVedleggUrls)
        }

        logger.trace("Totalt ${komplettDokumentUrls.size} dokumenter")
        logger.trace(komplettDokumentUrls.joinToString { it.toString() })

        logger.trace("Oppretter oppgave i Gosys")

        gosysService.opprett(
            sokerAktoerId = sokerAktoerId,
            barnAktoerId = barnAktoerId,
            mottatt = melding.mottatt,
            dokumenter = komplettDokumentUrls.toList(),
            correlationId = correlationId
        )

        logger.trace("Oppgave i Gosys opprettet OK")
    }
}