package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.CorrelationId
import no.nav.helse.k9mellomlagring.DokumentEier
import no.nav.helse.k9mellomlagring.K9MellomlagringService
import no.nav.helse.kafka.*
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.slf4j.LoggerFactory

internal class CleanupStream(
    kafkaConfig: KafkaConfig,
    k9MellomlagringService: K9MellomlagringService
) {
    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(k9MellomlagringService),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {
        private const val NAME = "CleanupV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(k9MellomlagringService: K9MellomlagringService): Topology {
            val builder = StreamsBuilder()
            val fraCleanup: Topic<TopicEntry<Cleanup>> = Topics.CLEANUP

            builder
                .stream<String, TopicEntry<Cleanup>>(
                    fraCleanup.name, Consumed.with(fraCleanup.keySerde, fraCleanup.valueSerde)
                )
                .filter {_, entry -> 1 == entry.metadata.version }
                .filter { _, entry -> entry.metadata.correlationId != "generated-58bec3e3-66f6-45a0-bbe2-fb23ce858677" }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        logger.info("Sletter dokumenter.")
                        k9MellomlagringService.slettDokumeter(
                            urlBolks = entry.data.melding.dokumentUrls,
                            dokumentEier = DokumentEier(entry.data.melding.søker.fødselsnummer),
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
