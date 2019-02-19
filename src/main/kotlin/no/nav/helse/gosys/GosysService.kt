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
    private val oppgaveGateway: OppgaveGateway,
    private val sakGateway: SakGateway
) {
    suspend fun opprett(
        sokerAktoerId: AktoerId,
        barnAktoerId: AktoerId?,
        mottatt: ZonedDateTime,
        dokumenter: List<URL>,
        correlationId: CorrelationId
    ) {
        logger.trace("oppretter sak")
        val sakId = sakGateway.lagSak(
            aktoerId = sokerAktoerId,
            correlationId = correlationId
        )
        logger.trace("sak opprettet med id ${sakId.sakId}")

        logger.trace("oppretter journalpost")
        val journalPostId = joarkGateway.journalfoer(
            aktoerId = sokerAktoerId,
            sakId = sakId,
            mottatt = mottatt,
            dokumenter = dokumenter,
            correlationId = correlationId
        )
        logger.trace("journalpost opprettet med id ${journalPostId.journalPostId}")

        logger.trace("oppretter oppgave")
        val oppgaveId = oppgaveGateway.lagOppgave(
            sokerAktoerId = sokerAktoerId,
            barnAktoerId = barnAktoerId,
            journalPostId = journalPostId,
            sakId = sakId,
            correlationId = correlationId
        )

        logger.trace("oppgave opprettet med id ${oppgaveId.oppgaveId}")
    }
}