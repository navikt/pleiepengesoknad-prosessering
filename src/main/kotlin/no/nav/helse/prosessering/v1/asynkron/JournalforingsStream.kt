package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.k9format.*
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.prosessering.v1.*
import no.nav.helse.prosessering.v1.Tilsynsordning
import no.nav.k9.søknad.felles.*
import no.nav.k9.søknad.felles.Barn
import no.nav.k9.søknad.pleiepengerbarn.*
import no.nav.k9.søknad.pleiepengerbarn.Beredskap
import no.nav.k9.søknad.pleiepengerbarn.Utenlandsopphold
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory
import java.math.BigDecimal

internal class JournalforingsStream(
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
        private const val NAME = "JournalforingV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(joarkGateway: JoarkGateway): Topology {
            val builder = StreamsBuilder()
            val fraPreprossesert: Topic<TopicEntry<PreprossesertMeldingV1>> = Topics.PREPROSSESERT
            val tilCleanup: Topic<TopicEntry<Cleanup>> = Topics.CLEANUP

            builder
                .stream<String, TopicEntry<PreprossesertMeldingV1>>(
                    fraPreprossesert.name,
                    Consumed.with(fraPreprossesert.keySerde, fraPreprossesert.valueSerde)
                )
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        logger.info("Journalfører dokumenter.")
                        val journaPostId = joarkGateway.journalfoer(
                            mottatt = entry.data.mottatt,
                            aktoerId = AktoerId(entry.data.soker.aktoerId),
                            correlationId = CorrelationId(entry.metadata.correlationId),
                            dokumenter = entry.data.dokumentUrls,
                            norskIdent = entry.data.soker.fodselsnummer
                        )
                        logger.info("Dokumenter journalført med ID = ${journaPostId.journalPostId}.")
                        val journalfort = Journalfort(
                            journalpostId = journaPostId.journalPostId,
                            søknad = entry.data.tilK9PleiepengeBarnSøknad()
                        )
                        Cleanup(
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
