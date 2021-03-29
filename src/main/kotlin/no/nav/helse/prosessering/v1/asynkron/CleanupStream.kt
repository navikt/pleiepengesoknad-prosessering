package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.dokument.DokumentService
import no.nav.helse.kafka.*
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.k9.søknad.JsonUtils
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory

internal class CleanupStream(
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
        private const val NAME = "CleanupV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(dokumentService: DokumentService): Topology {
            val builder = StreamsBuilder()
            val fraCleanup: Topic<TopicEntry<Cleanup>> = Topics.CLEANUP

            builder
                .stream<String, TopicEntry<Cleanup>>(
                    fraCleanup.name, Consumed.with(fraCleanup.keySerde, fraCleanup.valueSerde)
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
                        val journalførtMelding = entry.data.journalførtMelding
                        // TODO: 16/02/2021 Må fjernes før prodsetting
                        logger.info(JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(journalførtMelding))
                    }
                }
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}
