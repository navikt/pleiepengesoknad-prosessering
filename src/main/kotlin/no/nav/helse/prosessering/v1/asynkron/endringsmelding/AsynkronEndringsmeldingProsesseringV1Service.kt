package no.nav.helse.prosessering.v1.asynkron.endringsmelding

import no.nav.helse.joark.JoarkGateway
import no.nav.helse.k9mellomlagring.K9MellomlagringService
import no.nav.helse.kafka.KafkaConfig
import org.slf4j.LoggerFactory

internal class AsynkronEndringsmeldingProsesseringV1Service(
    kafkaConfig: KafkaConfig,
    endringsmeldingPreprosseseringV1Service: EndringsmeldingPreprosseseringV1Service,
    joarkGateway: JoarkGateway,
    k9MellomlagringService: K9MellomlagringService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(AsynkronEndringsmeldingProsesseringV1Service::class.java)
    }

    private val endringsmeldingPreprosseseringStream = EndringsmeldingPreprosseseringStream(
        kafkaConfig = kafkaConfig,
        endringsmeldingPreprosseseringV1Service = endringsmeldingPreprosseseringV1Service
    )

    private val endringsmeldingJournalforingsStream = EndringsmeldingJournalforingsStream(
        kafkaConfig = kafkaConfig,
        joarkGateway = joarkGateway
    )

    private val endringsmeldingCleanupStream = EndringsmeldingCleanupStream(
        kafkaConfig = kafkaConfig,
        k9MellomlagringService = k9MellomlagringService
    )

    private val healthChecks = setOf(
        endringsmeldingPreprosseseringStream.healthy,
        endringsmeldingJournalforingsStream.healthy,
        endringsmeldingCleanupStream.healthy
    )

    private val isReadyChecks = setOf(
        endringsmeldingPreprosseseringStream.ready,
        endringsmeldingJournalforingsStream.ready,
        endringsmeldingCleanupStream.ready
    )

    internal fun stop() {
        logger.info("Stopper streams.")
        endringsmeldingPreprosseseringStream.stop()
        endringsmeldingJournalforingsStream.stop()
        endringsmeldingCleanupStream.stop()
        logger.info("Alle streams stoppet.")
    }

    internal fun healthChecks() = healthChecks
    internal fun isReadyChecks() = isReadyChecks
}
