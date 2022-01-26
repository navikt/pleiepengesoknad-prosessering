package no.nav.helse.prosessering.v1.asynkron.endringsmelding

import no.nav.helse.CorrelationId
import no.nav.helse.felles.tilTpsNavn
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.kafka.TopicEntry
import no.nav.helse.kafka.process
import no.nav.helse.prosessering.v1.asynkron.CleanupEndringsmelding
import no.nav.helse.prosessering.v1.asynkron.EndringsmeldingTopics
import no.nav.helse.prosessering.v1.asynkron.Journalfort
import no.nav.helse.prosessering.v1.asynkron.Topic
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory

internal class EndringsmeldingJournalforingsStream(
    joarkGateway: JoarkGateway,
    kafkaConfig: KafkaConfig
) {

    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(joarkGateway),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {
        private const val NAME = "EndringsmeldingJournalforingV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(joarkGateway: JoarkGateway): Topology {
            val builder = StreamsBuilder()
            val fraPreprossesert: Topic<TopicEntry<PreprossesertEndringsmeldingV1>> = EndringsmeldingTopics.ENDRINGSMELDING_PREPROSSESERT
            val tilCleanup: Topic<TopicEntry<CleanupEndringsmelding>> = EndringsmeldingTopics.ENDRINGSMELDING_CLEANUP

            builder
                .stream<String, TopicEntry<PreprossesertEndringsmeldingV1>>(
                    fraPreprossesert.name,
                    Consumed.with(fraPreprossesert.keySerde, fraPreprossesert.valueSerde)
                )
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        logger.info("Journalfører dokumenter.")
                        val journaPostId = joarkGateway.journalfoer(
                            mottatt = entry.data.k9FormatSøknad.mottattDato,
                            sokerNavn = entry.data.søker.tilTpsNavn(),
                            correlationId = CorrelationId(entry.metadata.correlationId),
                            dokumentId = entry.data.dokumentId,
                            norskIdent = entry.data.søker.fødselsnummer
                        )
                        logger.info("Dokumenter journalført med ID = ${journaPostId.journalPostId}.")
                        val journalfort = Journalfort(
                            journalpostId = journaPostId.journalPostId,
                            søknad = entry.data.k9FormatSøknad
                        )
                        CleanupEndringsmelding(
                            metadata = entry.metadata,
                            melding = entry.data,
                            journalførtMelding = journalfort
                        )
                    }
                }
                .to(tilCleanup.name, Produced.with(tilCleanup.keySerde, tilCleanup.valueSerde))
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}
