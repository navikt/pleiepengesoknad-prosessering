package no.nav.helse.prosessering.v1.asynkron.endringsmelding

import no.nav.helse.CorrelationId
import no.nav.helse.dokument.DokumentService
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.kafka.TopicEntry
import no.nav.helse.kafka.process
import no.nav.helse.prosessering.v1.asynkron.CleanupEndringsmelding
import no.nav.helse.prosessering.v1.asynkron.EndringsmeldingTopics
import no.nav.helse.prosessering.v1.asynkron.Topic
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.slf4j.LoggerFactory

internal class EndringsmeldingCleanupStream(
    kafkaConfig: KafkaConfig,
    dokumentService: DokumentService
) {
    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(dokumentService),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {
        private const val NAME = "EndringsmeldingCleanupV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(dokumentService: DokumentService): Topology {
            val builder = StreamsBuilder()
            val fraCleanup: Topic<TopicEntry<CleanupEndringsmelding>> = EndringsmeldingTopics.ENDRINGSMELDING_CLEANUP

            builder
                .stream<String, TopicEntry<CleanupEndringsmelding>>(
                    fraCleanup.name, Consumed.with(fraCleanup.keySerde, fraCleanup.valueSerde)
                )
                .filter {_, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        logger.info("Sletter dokumenter tilknyttet endringsmeldimg.")

                        dokumentService.slettDokumeter(
                            urlBolks = entry.data.melding.dokumentUrls,
                            aktørId = entry.data.melding.søker.aktørId,
                            correlationId = CorrelationId(entry.metadata.correlationId)
                        )
                        logger.info("Dokumenter slettet.")
                    }
                }
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}
