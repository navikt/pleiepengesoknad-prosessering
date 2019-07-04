package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.joark.JoarkGateway
import no.nav.helse.oppgave.OppgaveGateway
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.SoknadId
import no.nav.helse.prosessering.v1.PreprosseseringV1Service
import no.nav.helse.prosessering.v1.ProsesseringV1Service
import org.slf4j.LoggerFactory

internal class AsynkronProsesseringV1Service(
    kafkaConfig: KafkaConfig,
    preprosseseringV1Service: PreprosseseringV1Service,
    joarkGateway: JoarkGateway,
    oppgaveGateway: OppgaveGateway
) : ProsesseringV1Service {

    private companion object {
        private val logger = LoggerFactory.getLogger(AsynkronProsesseringV1Service::class.java)
    }

    private val producer = SoknadProducer(kafkaConfig)

    private val preprosseseringStream = PreprosseseringStream(
        kafkaConfig = kafkaConfig,
        preprosseseringV1Service = preprosseseringV1Service
    )

    private val journalforingsStream = JournalforingsStream(
        kafkaConfig = kafkaConfig,
        joarkGateway = joarkGateway
    )

    private val opprettOppgaveStream = OpprettOppgaveStream(
        kafkaConfig = kafkaConfig,
        oppgaveGateway = oppgaveGateway
    )

    private val healthChecks = setOf(
        preprosseseringStream.healthCheck(),
        journalforingsStream.healthCheck(),
        opprettOppgaveStream.healthCheck()
    )

    override suspend fun leggSoknadTilProsessering(
        melding: MeldingV1,
        metadata: Metadata
    ) : SoknadId {
        val soknadId = SoknadId.generate()
        producer.produce(
            soknadId = soknadId,
            metadata = metadata,
            melding = melding
        )
        return soknadId
    }

    internal fun stop() {
        logger.info("Stopper streams.")
        preprosseseringStream.stop()
        journalforingsStream.stop()
        opprettOppgaveStream.stop()
        logger.info("Alle streams stoppet.")
    }

    internal fun healthChecks() = healthChecks
}