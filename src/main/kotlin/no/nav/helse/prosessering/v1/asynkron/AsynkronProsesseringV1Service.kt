package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.dokument.DokumentService
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.prosessering.v1.PreprosseseringV1Service
import org.slf4j.LoggerFactory

internal class AsynkronProsesseringV1Service(
    kafkaConfig: KafkaConfig,
    preprosseseringV1Service: PreprosseseringV1Service,
    joarkGateway: JoarkGateway,
    dokumentService: DokumentService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(AsynkronProsesseringV1Service::class.java)
    }

    private val preprosseseringStream = PreprosseseringStream(
        kafkaConfig = kafkaConfig,
        preprosseseringV1Service = preprosseseringV1Service
    )

    private val journalforingsStream = JournalforingsStream(
        kafkaConfig = kafkaConfig,
        joarkGateway = joarkGateway
    )

    private val cleanupStream = CleanupStream(
        kafkaConfig = kafkaConfig,
        dokumentService = dokumentService
    )

    private val healthChecks = setOf(
        preprosseseringStream.healthy,
        journalforingsStream.healthy,
        cleanupStream.healthy
    )

    private val isReadyChecks = setOf(
        preprosseseringStream.ready,
        journalforingsStream.ready,
        cleanupStream.ready
    )

    internal fun stop() {
        logger.info("Stopper streams.")
        preprosseseringStream.stop()
        journalforingsStream.stop()
        cleanupStream.stop()
        logger.info("Alle streams stoppet.")
    }

    internal fun healthChecks() = healthChecks
    internal fun isReadyChecks() = isReadyChecks
}