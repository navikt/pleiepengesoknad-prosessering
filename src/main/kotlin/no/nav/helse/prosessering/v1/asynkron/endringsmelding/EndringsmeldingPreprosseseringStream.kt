package no.nav.helse.prosessering.v1.asynkron.endringsmelding

import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.kafka.TopicEntry
import no.nav.helse.kafka.process
import no.nav.helse.prosessering.v1.asynkron.EndringsmeldingTopics
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory

internal class EndringsmeldingPreprosseseringStream(
    endringsmeldingPreprosseseringV1Service: EndringsmeldingPreprosseseringV1Service,
    kafkaConfig: KafkaConfig
) {
    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(endringsmeldingPreprosseseringV1Service),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {

        private const val NAME = "EndringsmeldingPreprosesseringV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(endringsmeldingPreprosseseringV1Service: EndringsmeldingPreprosseseringV1Service): Topology {
            val builder = StreamsBuilder()
            val fromTopic = EndringsmeldingTopics.ENDRINGSMELDING_MOTTATT
            val toTopic = EndringsmeldingTopics.ENDRINGSMELDING_PREPROSSESERT

            builder
                .stream<String, TopicEntry<EndringsmeldingV1>>(
                    fromTopic.name,
                    Consumed.with(fromTopic.keySerde, fromTopic.valueSerde)
                )
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        logger.info("Preprosesserer endringsmelding.")
                        val preprossesertMelding: PreprossesertEndringsmeldingV1 =
                            endringsmeldingPreprosseseringV1Service.preprosseser(
                                endringsmelding = entry.data,
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
