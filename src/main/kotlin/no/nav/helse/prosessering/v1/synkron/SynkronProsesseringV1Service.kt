package no.nav.helse.prosessering.v1.synkron

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.oppgave.OppgaveGateway
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.SoknadId
import no.nav.helse.prosessering.v1.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger(SynkronProsesseringV1Service::class.java)

internal class SynkronProsesseringV1Service(
    private val preprosseseringV1Service: PreprosseseringV1Service,
    private val joarkGateway: JoarkGateway,
    private val oppgaveGateway: OppgaveGateway
) : ProsesseringV1Service {
    override suspend fun leggSoknadTilProsessering(
        melding: MeldingV1,
        metadata: Metadata
    ) : SoknadId {
        logger.info("Stratert prosessering.")

        val correlationId = CorrelationId(metadata.correlationId)
        val soknadId = SoknadId.generate()

        val preprossesertMelding = preprosseseringV1Service.preprosseser(
            melding = melding,
            metadata = metadata,
            soknadId = soknadId
        )

        val sokerAktoerId = AktoerId(preprossesertMelding.soker.aktoerId)
        val barnAktoerId = if (preprossesertMelding.barn.aktoerId != null) AktoerId(preprossesertMelding.barn.aktoerId) else null

        logger.trace("Oppretter JournalPost")
        val journalPostId = joarkGateway.journalfoer(
            aktoerId = sokerAktoerId,
            mottatt = preprossesertMelding.mottatt,
            dokumenter = preprossesertMelding.dokumentUrls,
            correlationId = correlationId
        )
        logger.info("Opprettet JournalPostID = $journalPostId")

        logger.trace("Oppretter Oppgave")
        val oppgaveId = oppgaveGateway.lagOppgave(
            sokerAktoerId = sokerAktoerId,
            barnAktoerId = barnAktoerId,
            journalPostId = journalPostId,
            correlationId = correlationId
        )

        logger.info("Opprettet OppgaveID = $oppgaveId")

        logger.trace("Prosessering ferdigstilt.")
        return soknadId
    }
}
