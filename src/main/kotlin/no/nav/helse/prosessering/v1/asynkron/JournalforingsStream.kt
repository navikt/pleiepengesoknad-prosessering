package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.CorrelationId
import no.nav.helse.HttpError
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.PauseableKafkaStreams
import no.nav.helse.prosessering.v1.PreprossesertMeldingV1
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory

internal class JournalforingsStream(
    joarkGateway: JoarkGateway,
    kafkaConfig: KafkaConfig
) {

    private val stream = PauseableKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(joarkGateway),
        pauseOn = { throwable ->
            throwable is HttpError && throwable.pauseStream()
        }
    )

    private companion object {
        private const val NAME = "JournalforingV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(joarkGateway: JoarkGateway) : Topology {
            val builder = StreamsBuilder()
            val fromTopic = Topics.PREPROSSESERT
            val toTopic = Topics.JOURNALFORT

            builder
                .stream<String, TopicEntry<PreprossesertMeldingV1>>(fromTopic.name, Consumed.with(fromTopic.keySerde, fromTopic.valueSerde))
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry  ->
                    runBlockingWithMDC(soknadId, entry) {
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
                .to(toTopic.name, Produced.with(toTopic.keySerde, toTopic.valueSerde))

            return builder.build()
        }
    }

    internal fun stop() = stream.stop()
    internal fun healthCheck() : HealthCheck = stream
}

private fun HttpError.pauseStream() = httpStatusCode() == null || httpStatusCode()!!.value >= 500
