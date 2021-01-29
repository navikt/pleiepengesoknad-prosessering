package no.nav.helse.prosessering.v2.asynkron

import no.nav.helse.dokument.DokumentService
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.prosessering.v2.PreprosseseringV2Service
import org.slf4j.LoggerFactory

internal class AsynkronProsesseringV2Service(
    kafkaConfig: KafkaConfig,
    preprosseseringV2Service: PreprosseseringV2Service,
    joarkGateway: JoarkGateway,
    dokumentService: DokumentService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(AsynkronProsesseringV2Service::class.java)
    }

    private val preprosseseringStream = PreprosseseringStreamV2(
        kafkaConfig = kafkaConfig,
        preprosseseringV1Service = preprosseseringV2Service
    )

    private val journalforingsStream = JournalforingsStreamV2(
        kafkaConfig = kafkaConfig,
        joarkGateway = joarkGateway
    )

    private val cleanupStream = CleanupStreamV2(
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
