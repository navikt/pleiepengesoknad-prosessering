package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.HttpError
import no.nav.helse.kafka.PauseableKafkaStreams
import no.nav.helse.prosessering.SoknadId
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.PreprosseseringV1Service
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory
import java.util.*

internal class PreprosseseringStream(
    preprosseseringV1Service: PreprosseseringV1Service,
    kafkaProperties : Properties
) {

    val stream = PauseableKafkaStreams(
        name = "PreprosseseringStreamV1",
        properties = kafkaProperties,
        topology = topology(preprosseseringV1Service),
        pauseOn = { throwable ->
            throwable is HttpError
        }
    )

    private companion object {
        private val logger = LoggerFactory.getLogger(PreprosseseringStream::class.java)

        private fun topology(preprosseseringV1Service: PreprosseseringV1Service) : Topology {
            val builder = StreamsBuilder()
            val fromTopic = Topics.MOTTATT
            val toTopic = Topics.PREPROSSESERT

            builder
                .stream<String, TopicEntry<MeldingV1>>(fromTopic.name, Consumed.with(fromTopic.keySerde, fromTopic.valueSerde))
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry  ->
                    runBlockingWithMDC(soknadId, entry) {
                        logger.trace("Sender søknad $soknadId til prepprosessering.")
                        preprosseseringV1Service.preprosseser(
                            melding = entry.data,
                            metadata = entry.metadata,
                            soknadId = SoknadId(soknadId)
                        )
                    }
                }
                .to(toTopic.name, Produced.with(toTopic.keySerde, toTopic.valueSerde))

            return builder.build()
        }
    }

    internal fun stop() = stream.stop()
}

