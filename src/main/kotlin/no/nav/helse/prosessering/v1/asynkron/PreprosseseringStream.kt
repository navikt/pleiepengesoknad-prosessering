package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.HttpError
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.PauseableKafkaStreams
import no.nav.helse.prosessering.SoknadId
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.PreprosseseringV1Service
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory

internal class PreprosseseringStream(
    preprosseseringV1Service: PreprosseseringV1Service,
    kafkaConfig: KafkaConfig
) {
    private val stream = PauseableKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(preprosseseringV1Service),
        pauseOn = { throwable ->
            throwable is HttpError && throwable.pauseStream()
        }
    )

    private companion object {
        private const val NAME = "PreprosesseringV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(preprosseseringV1Service: PreprosseseringV1Service) : Topology {
            val builder = StreamsBuilder()
            val fromTopic = Topics.MOTTATT
            val toTopic = Topics.PREPROSSESERT

            builder
                .stream<String, TopicEntry<MeldingV1>>(fromTopic.name, Consumed.with(fromTopic.keySerde, fromTopic.valueSerde))
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry  ->
                    runBlockingWithMDC(soknadId, entry) {
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
                .to(toTopic.name, Produced.with(toTopic.keySerde, toTopic.valueSerde))

            return builder.build()
        }
    }

    internal fun stop() = stream.stop()
    internal fun healthCheck() : HealthCheck = stream
}

private fun HttpError.pauseStream() = httpStatusCode() == null || httpStatusCode()!!.value >= 500
