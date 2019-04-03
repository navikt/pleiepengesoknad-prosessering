package no.nav.helse.gosys

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.ZonedDateTime

private val logger: Logger = LoggerFactory.getLogger("nav.GosysService")

class GosysService(
    private val joarkGateway: JoarkGateway,
    private val oppgaveGateway: OppgaveGateway
) {
    suspend fun opprett(
        sokerAktoerId: AktoerId,
        barnAktoerId: AktoerId?,
        mottatt: ZonedDateTime,
        dokumenter: List<List<URL>>,
        correlationId: CorrelationId
    ) {
        logger.trace("Oppretter JournalPost")
        val journalPostId = joarkGateway.journalfoer(
            aktoerId = sokerAktoerId,
            mottatt = mottatt,
            dokumenter = dokumenter,
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
    }
}