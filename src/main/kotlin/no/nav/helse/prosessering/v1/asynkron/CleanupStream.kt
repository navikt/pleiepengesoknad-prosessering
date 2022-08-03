package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.CorrelationId
import no.nav.helse.k9mellomlagring.DokumentEier
import no.nav.helse.k9mellomlagring.K9MellomlagringService
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.kafka.TopicEntry
import no.nav.helse.kafka.process
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
            val fraCleanup: Topic<TopicEntry<Cleanup>> = SøknadTopics.CLEANUP

            builder
                .stream<String, TopicEntry<Cleanup>>(
                    fraCleanup.name, Consumed.with(fraCleanup.keySerde, fraCleanup.valueSerde)
                )
                .filter {_, entry -> 1 == entry.metadata.version }
                .filterNot { _, entry ->
                    logger.info("Ignorer duplikat søknad med correlationId = 'generated-e39a87ae-67c9-4095-a955-3c983a245243'")
                    entry.metadata.correlationId == "generated-e39a87ae-67c9-4095-a955-3c983a245243"
                }
                .filterNot { _, entry ->
                    logger.info("Ignorer duplikat søknad med correlationId = 'generated-d7821930-3de6-48fd-a986-de0ba79a0632'")
                    entry.metadata.correlationId == "generated-d7821930-3de6-48fd-a986-de0ba79a0632"
                }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        logger.info("Sletter dokumenter.")
                        k9MellomlagringService.slettDokumeter(
                            dokumentIdBolks = entry.data.melding.dokumentId,
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
