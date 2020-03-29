package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.dokument.DokumentService
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.prosessering.v1.PreprosseseringV1Service
import no.nav.helse.prosessering.v1.asynkron.ettersending.CleanupStreamEttersending
import no.nav.helse.prosessering.v1.asynkron.ettersending.JournalføringStreamEttersending
import no.nav.helse.prosessering.v1.asynkron.ettersending.PreprosseseringStreamEttersending
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

    private val preprosseseringStreamEttersending = PreprosseseringStreamEttersending(
        kafkaConfig = kafkaConfig,
        preprosseseringV1Service = preprosseseringV1Service
    )
    private val cleanupStreamEttersending = CleanupStreamEttersending(
        kafkaConfig = kafkaConfig,
        dokumentService = dokumentService
    )
    private val journalforingsStreamEttersending = JournalføringStreamEttersending(
        kafkaConfig = kafkaConfig,
        joarkGateway = joarkGateway
    )

    private val healthChecks = setOf(
        preprosseseringStream.healthy,
        journalforingsStream.healthy,
        cleanupStream.healthy,
        preprosseseringStreamEttersending.healthy,
        cleanupStreamEttersending.healthy,
        journalforingsStreamEttersending.healthy
    )

    private val isReadyChecks = setOf(
        preprosseseringStream.ready,
        journalforingsStream.ready,
        cleanupStream.ready,
        preprosseseringStreamEttersending.ready,
        cleanupStreamEttersending.ready,
        journalforingsStreamEttersending.ready
    )

    internal fun stop() {
        logger.info("Stopper streams.")

        preprosseseringStream.stop()
        journalforingsStream.stop()
        cleanupStream.stop()

        preprosseseringStreamEttersending.stop()
        cleanupStreamEttersending.stop()
        journalforingsStreamEttersending.stop()

        logger.info("Alle streams stoppet.")
    }

    internal fun healthChecks() = healthChecks
    internal fun isReadyChecks() = isReadyChecks
}
