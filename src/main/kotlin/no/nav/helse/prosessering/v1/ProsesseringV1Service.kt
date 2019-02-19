package no.nav.helse.prosessering.v1

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerService
import no.nav.helse.aktoer.Fodselsnummer
import no.nav.helse.gosys.GosysService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("nav.ProsesseringV1Service")

class ProsesseringV1Service(
    private val gosysService: GosysService,
    private val aktoerService: AktoerService
) {
    suspend fun prosesser(
        melding: MeldingV1,
        metadata: MetadataV1
    ) {
        logger.info(metadata.toString())

        // TODO: Validere melding

        val correlationId = CorrelationId(metadata.correlationId)

        val barnAktoerId = if (melding.barn.fodselsnummer != null) {
            aktoerService.getAktorId(
                fnr = Fodselsnummer(melding.barn.fodselsnummer),
                correlationId = correlationId
            )
        } else null


        gosysService.opprett(
            sokerAktoerId = aktoerService.getAktorId(
                fnr = Fodselsnummer(melding.soker.fodselsnummer),
                correlationId = correlationId
            ),
            barnAktoerId = barnAktoerId,
            mottatt = melding.mottatt,
            dokumenter = melding.vedlegg, // TODO : Her må vi generere en PDF basert på innholdet i meldingen og legge det som dokument 1 til gosysService
            correlationId = correlationId
        )
    }
}