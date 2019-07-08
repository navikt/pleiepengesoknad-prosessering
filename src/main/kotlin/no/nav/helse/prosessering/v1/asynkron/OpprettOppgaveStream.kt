package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.joark.JournalPostId
import no.nav.helse.oppgave.OppgaveGateway
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory

internal class OpprettOppgaveStream(
    oppgaveGateway: OppgaveGateway,
    kafkaConfig: KafkaConfig
) {

    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(oppgaveGateway),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    private companion object {
        private const val NAME = "OpprettOppgaveV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(oppgaveGateway: OppgaveGateway) : Topology {
            val builder = StreamsBuilder()
            val fromTopic = Topics.JOURNALFORT
            val toTopic = Topics.OPPGAVE_OPPRETTET

            builder
                .stream<String, TopicEntry<Journalfort>>(fromTopic.name, Consumed.with(fromTopic.keySerde, fromTopic.valueSerde))
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry  ->
                    process(NAME, soknadId, entry) {
                        logger.info("Oppretter oppgave.")
                        val oppgaveId = oppgaveGateway.lagOppgave(
                            sokerAktoerId = AktoerId(entry.data.melding.soker.aktoerId),
                            barnAktoerId = if (entry.data.melding.barn.aktoerId != null) AktoerId(entry.data.melding.barn.aktoerId) else null,
                            journalPostId = JournalPostId(entry.data.journalPostId),
                            correlationId = CorrelationId(entry.metadata.correlationId)
                        )
                        logger.info("Opprettet oppgave med ID = ${oppgaveId.oppgaveId}")
                        OppgaveOpprettet(
                            oppgaveId = oppgaveId.oppgaveId,
                            journalPostId = entry.data.journalPostId,
                            melding = entry.data.melding
                        )
                    }
                }
                .to(toTopic.name, Produced.with(toTopic.keySerde, toTopic.valueSerde))
            return builder.build()
        }
    }

    internal fun stop() = stream.stop()
    internal fun healthCheck() : HealthCheck = stream
}