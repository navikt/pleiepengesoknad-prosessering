package no.nav.helse.prosessering.v2.asynkron

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.felles.tilTpsNavn
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.k9format.tilK9PleiepengesøknadSyktBarn
import no.nav.helse.kafka.*
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.prosessering.v2.PreprossesertMeldingV2
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory

internal class JournalforingsStreamV2(
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
        private const val NAME = "JournalforingV2"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(joarkGateway: JoarkGateway): Topology {
            val builder = StreamsBuilder()
            val fraPreprossesert= no.nav.helse.prosessering.v2.asynkron.TopicsV2.PREPROSSESERT
            val tilCleanup = TopicsV2.CLEANUP

            builder
                .stream<String, TopicEntry<PreprossesertMeldingV2>>(
                    fraPreprossesert.name,
                    Consumed.with(fraPreprossesert.keySerde, fraPreprossesert.valueSerde)
                )
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        logger.info("Journalfører dokumenter.")
                        val journaPostId = joarkGateway.journalfoer(
                            mottatt = entry.data.mottatt,
                            aktoerId = AktoerId(entry.data.søker.aktørId),
                            sokerNavn = entry.data.søker.tilTpsNavn(),
                            correlationId = CorrelationId(entry.metadata.correlationId),
                            dokumenter = entry.data.dokumentUrls,
                            norskIdent = entry.data.søker.fødselsnummer
                        )
                        logger.info("Dokumenter journalført med ID = ${journaPostId.journalPostId}.")
                        val journalfort = JournalfortV2(
                            journalpostId = journaPostId.journalPostId,
                            søknad = entry.data.tilK9PleiepengesøknadSyktBarn()
                        )
                        CleanupV2(
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
