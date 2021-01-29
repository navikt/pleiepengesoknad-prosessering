package no.nav.helse.prosessering.v2.asynkron

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.dokument.DokumentService
import no.nav.helse.kafka.*
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory

internal class CleanupStreamV2(
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
        private const val NAME = "CleanupV2"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(dokumentService: DokumentService): Topology {
            val builder = StreamsBuilder()
            val fraCleanupV2: Topic<TopicEntry<CleanupV2>> = TopicsV2.CLEANUP
            val tilJournalfortV2: Topic<TopicEntry<JournalfortV2>> = TopicsV2.JOURNALFORT

            builder
                .stream<String, TopicEntry<CleanupV2>>(
                    fraCleanupV2.name, Consumed.with(fraCleanupV2.keySerde, fraCleanupV2.valueSerde)
                )
                .filter {_, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        logger.info("Sletter dokumenter.")

                        dokumentService.slettDokumeter(
                            urlBolks = entry.data.melding.dokumentUrls,
                            aktørId = AktoerId(entry.data.melding.søker.aktørId),
                            correlationId = CorrelationId(entry.metadata.correlationId)
                        )
                        logger.info("Dokumenter slettet.")
                        logger.info("Videresender journalført melding")
                        entry.data.journalførtMelding
                    }
                }
                .to(tilJournalfortV2.name, Produced.with(tilJournalfortV2.keySerde, tilJournalfortV2.valueSerde))
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}
