package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.kafka.*
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.prosessering.v2.MeldingV2
import no.nav.helse.prosessering.v2.PreprosseseringV2Service
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory

internal class PreprosseseringStreamV2(
    preprosseseringV1Service: PreprosseseringV2Service,
    kafkaConfig: KafkaConfig
) {
    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(preprosseseringV1Service),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {

        private const val NAME = "PreprosesseringV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(preprosseseringV2Service: PreprosseseringV2Service) : Topology {
            val builder = StreamsBuilder()
            val fromTopic = no.nav.helse.prosessering.v2.asynkron.Topics.MOTTATT
            val toTopic = no.nav.helse.prosessering.v2.asynkron.Topics.PREPROSSESERT

            builder
                .stream<String, TopicEntry<MeldingV2>>(fromTopic.name, Consumed.with(fromTopic.keySerde, fromTopic.valueSerde))
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry  ->
                    process(NAME, soknadId, entry) {
                        logger.info("Preprosesserer søknad.")
                        val preprossesertMelding = preprosseseringV2Service.preprosseser(
                            melding = entry.data,
                            metadata = entry.metadata
                        )
                        logger.info("Preprossesering ferdig.")
                        preprossesertMelding
                    }
                }
                .to(toTopic.name, Produced.with(toTopic.keySerde, toTopic.valueSerde))
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}
