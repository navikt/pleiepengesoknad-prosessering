package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.CorrelationId
import no.nav.helse.HttpError
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.gosys.JournalPostId
import no.nav.helse.gosys.OppgaveGateway
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.PauseableKafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory

internal class OpprettOppgaveStream(
    oppgaveGateway: OppgaveGateway,
    kafkaConfig: KafkaConfig
) {

    private val stream = PauseableKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(oppgaveGateway),
        pauseOn = { throwable ->
            throwable is HttpError && throwable.pauseStream()
        }
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
                    runBlockingWithMDC(soknadId, entry) {
                        logger.trace("Oppretter oppgave.")
                        val oppgaveId = oppgaveGateway.lagOppgave(
                            sokerAktoerId = AktoerId(entry.data.melding.soker.aktoerId),
                            barnAktoerId = if (entry.data.melding.barn.aktoerId != null) AktoerId(entry.data.melding.barn.aktoerId) else null,
                            journalPostId = JournalPostId(entry.data.journalPostId),
                            correlationId = CorrelationId(entry.metadata.correlationId)
                        )
                        logger.trace("Oppgave opprettet.")
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
}

private fun HttpError.pauseStream() = httpStatusCode() == null || httpStatusCode()!!.value >= 500
