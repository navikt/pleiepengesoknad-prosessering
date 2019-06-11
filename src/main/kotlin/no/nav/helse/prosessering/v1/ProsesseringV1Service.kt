package no.nav.helse.prosessering.v1

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerId
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

        melding.validate()

        val correlationId = CorrelationId(metadata.correlationId)

        logger.trace("Henter AktørID for søkeren.")
        val sokerAktoerId = aktoerService.getAktorId(
            fnr = Fodselsnummer(melding.soker.fodselsnummer),
            correlationId = correlationId
        )

        logger.info("Søkerens AktørID = $sokerAktoerId")

        logger.trace("Henter AktørID for barnet.")
        val barnAktoerId = hentBarnetsAktoerId(barn = melding.barn, correlationId = correlationId)

        logger.info("Barnets AktørID = $barnAktoerId")

        logger.trace("Genererer Oppsummerings-PDF av søknaden.")

        val soknadOppsummeringPdf = pdfV1Generator.generateSoknadOppsummeringPdf(melding)

        logger.trace("Generering av Oppsummerings-PDF OK.")
        logger.trace("Mellomlagrer Oppsummerings-PDF.")

        val soknadOppsummeringPdfUrl = dokumentService.lagreSoknadsOppsummeringPdf(
            pdf = soknadOppsummeringPdf,
            correlationId = correlationId,
            aktoerId = sokerAktoerId
        )

        logger.trace("Mellomlagring av Oppsummerings-PDF OK")

        logger.trace("Mellomlagrer Oppsummerings-JSON")

        val soknadJsonUrl = dokumentService.lagreSoknadsMelding(
            melding = melding,
            aktoerId = sokerAktoerId,
            correlationId = correlationId
        )

        logger.trace("Mellomlagrer Oppsummerings-JSON OK.")


        val komplettDokumentUrls = mutableListOf(
            listOf(
                soknadOppsummeringPdfUrl,
                soknadJsonUrl
            )
        )

        if (melding.vedleggUrls.isNotEmpty()) {
            logger.trace("Legger til ${melding.vedleggUrls.size} vedlegg URL's fra meldingen som dokument.")
            melding.vedleggUrls.forEach { komplettDokumentUrls.add(listOf(it))}
        }
        if (melding.vedlegg.isNotEmpty()) {
            logger.trace("Meldingen inneholder ${melding.vedlegg.size} vedlegg som må mellomlagres før søknaden legges til prosessering.")
            val lagredeVedleggUrls = dokumentService.lagreVedlegg(
                vedlegg = melding.vedlegg,
                correlationId = correlationId,
                aktoerId = sokerAktoerId
            )
            logger.trace("Mellomlagring OK, legger til URL's som dokument.")
            lagredeVedleggUrls.forEach { it -> komplettDokumentUrls.add(listOf(it))}
        }

        logger.trace("Totalt ${komplettDokumentUrls.size} dokumentbolker.")

        logger.trace("Oppretter oppgave i Gosys")

        val komplettDokumentUrlsList = komplettDokumentUrls.toList()

        gosysService.opprett(
            sokerAktoerId = sokerAktoerId,
            barnAktoerId = barnAktoerId,
            mottatt = melding.mottatt,
            dokumenter = komplettDokumentUrlsList,
            correlationId = correlationId
        )

        logger.trace("Oppgave i Gosys opprettet OK")

        // Reporterer metrics først når oppgave er opprettet OK.
        melding.reportMetrics()

        logger.trace("Sletter dokumenter.")
        try { dokumentService.slettDokumeter(
            urlBolks = komplettDokumentUrlsList,
            aktoerId = sokerAktoerId,
            correlationId = correlationId
        )} catch (cause: Throwable) {
            logger.warn("Feil ved sletting av dokumenter etter ferdigstilt prosessering", cause)
        }

        logger.trace("Prosessering ferdigstilt.")
    }

    private suspend fun hentBarnetsAktoerId(
        barn: Barn,
        correlationId: CorrelationId
    ): AktoerId? {
         return if (barn.fodselsnummer != null) {
            try {
                aktoerService.getAktorId(
                    fnr = Fodselsnummer(barn.fodselsnummer),
                    correlationId = correlationId
                )
            } catch (cause: Throwable) {
                logger.warn("Feil ved oppslag på Aktør ID basert på barnets fødselsnummer. Kan være at det ikke er registrert i Aktørregisteret enda. ${cause.message}")
                null
            }
        } else null
    }
}
