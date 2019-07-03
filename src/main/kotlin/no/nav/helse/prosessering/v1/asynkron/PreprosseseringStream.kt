package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.prosessering.SoknadId
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.PreprosseseringV1Service
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Predicate
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory

internal class PreprosseseringStream(
    preprosseseringV1Service: PreprosseseringV1Service,
    kafkaConfig: KafkaConfig
) {
    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(preprosseseringV1Service)
    )

    private companion object {

        private const val NAME = "PreprosesseringV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(preprosseseringV1Service: PreprosseseringV1Service) : Topology {
            val builder = StreamsBuilder()
            val fromTopic = Topics.MOTTATT
            val toTopic = Topics.PREPROSSESERT

            val (ok, tryAgain, exhausted) = builder
                .stream<String, TopicEntry<MeldingV1>>(fromTopic.name, Consumed.with(fromTopic.keySerde, fromTopic.valueSerde))
                .filter { _, entry -> 1 == entry.metadata.version }
                .peek { soknadId, entry -> peekAttempts(soknadId, entry, logger) }
                .mapValues { soknadId, entry  ->
                    process(soknadId, entry, logger) {
                        logger.trace("Sender søknad til prepprosessering.")
                        val preprossesertMelding = preprosseseringV1Service.preprosseser(
                            melding = entry.data,
                            metadata = entry.metadata,
                            soknadId = SoknadId(soknadId)
                        )
                        logger.trace("Søknad preprossesert.")
                        preprossesertMelding
                    }
                }
                .branch(
                    Predicate { _, result -> result.ok() },
                    Predicate { _, result -> result.tryAgain() },
                    Predicate { _, result -> result.exhausted() }
                )

            ok
                .mapValues { _, value ->
                    value.after()
                }.to(toTopic.name, Produced.with(toTopic.keySerde, toTopic.valueSerde))

            tryAgain
                .mapValues { _, value ->
                    value.before()
                }.to(fromTopic.name, Produced.with(fromTopic.keySerde, fromTopic.valueSerde))

            exhausted
                .mapValues { _, value ->
                    logger.error("Exhausted TopicEntry='${rawTopicEntry(value.before())}'")
                }
            return builder.build()
        }
    }

    internal fun stop() = stream.stop()
    internal fun healthCheck() : HealthCheck = stream
}