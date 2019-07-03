package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.prosessering.v1.PreprossesertMeldingV1
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Predicate
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory

internal class JournalforingsStream(
    joarkGateway: JoarkGateway,
    kafkaConfig: KafkaConfig
) {

    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(joarkGateway)
    )

    private companion object {
        private const val NAME = "JournalforingV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(joarkGateway: JoarkGateway) : Topology {
            val builder = StreamsBuilder()
            val fromTopic = Topics.PREPROSSESERT
            val toTopic = Topics.JOURNALFORT

            val (ok, tryAgain, exhausted) = builder
                .stream<String, TopicEntry<PreprossesertMeldingV1>>(fromTopic.name, Consumed.with(fromTopic.keySerde, fromTopic.valueSerde))
                .filter { _, entry -> 1 == entry.metadata.version }
                .peek { soknadId, entry -> peekAttempts(soknadId, entry, logger) }
                .mapValues { soknadId, entry  ->
                    process(soknadId, entry, logger) {
                        logger.trace("Journalfører dokumenter.")
                        val journaPostId = joarkGateway.journalfoer(
                            mottatt = entry.data.mottatt,
                            aktoerId = AktoerId(entry.data.soker.aktoerId),
                            correlationId = CorrelationId(entry.metadata.correlationId),
                            dokumenter = entry.data.dokumentUrls
                        )
                        logger.trace("Dokumenter journalført.")
                        Journalfort(
                            journalPostId = journaPostId.journalPostId,
                            melding = entry.data
                        )
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