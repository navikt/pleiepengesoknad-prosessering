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
        logger.trace("oppretter journalpost")
        val journalPostId = joarkGateway.journalfoer(
            aktoerId = sokerAktoerId,
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
            correlationId = correlationId
        )

        logger.trace("oppgave opprettet med id ${oppgaveId.oppgaveId}")
    }
}