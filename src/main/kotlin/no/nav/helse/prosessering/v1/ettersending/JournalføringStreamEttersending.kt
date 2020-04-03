package no.nav.helse.prosessering.v1.asynkron.ettersending

import no.nav.helse.prosessering.v1.asynkron.*
import no.nav.helse.prosessering.v1.asynkron.Topic
import no.nav.helse.prosessering.v1.asynkron.Topics
import no.nav.helse.prosessering.v1.asynkron.process
import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.prosessering.v1.PreprossesertSoker
import no.nav.helse.prosessering.v1.ettersending.PreprossesertEttersending
import no.nav.k9.ettersendelse.Ettersendelse
import no.nav.k9.ettersendelse.Ytelse
import no.nav.k9.søknad.felles.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.Søker
import no.nav.k9.søknad.felles.SøknadId
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory

internal class JournalføringStreamEttersending(
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
        private const val NAME = "JournalforingV1Ettersending"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(joarkGateway: JoarkGateway): Topology {
            val builder = StreamsBuilder()
            val fraPreprossesert: Topic<TopicEntry<PreprossesertEttersending>> = Topics.ETTERSENDING_PREPROSSESERT
            val tilCleanup: Topic<TopicEntry<CleanupEttersending>> = Topics.ETTERSENDING_CLEANUP

            val mapValues = builder
                .stream(fraPreprossesert.name, Consumed.with(fraPreprossesert.keySerde, fraPreprossesert.valueSerde))
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {

                        val dokumenter = entry.data.dokumentUrls
                        logger.info("Journalfører dokumenter for ettersending: {}", dokumenter)
                        val journaPostId = joarkGateway.journalførEttersending(
                            mottatt = entry.data.mottatt,
                            aktørId = AktoerId(entry.data.soker.aktoerId),
                            sokerNavn = entry.data.soker.tilTpsNavn(),
                            norskIdent = entry.data.soker.fodselsnummer,
                            correlationId = CorrelationId(entry.metadata.correlationId),
                            dokumenter = dokumenter
                        )
                        logger.info("Dokumenter for ettersending journalført med ID = ${journaPostId.journalPostId}.")
                        val journalfort = JournalførtEttersending(
                            journalpostId = journaPostId.journalPostId,
                            søknad = entry.data.tilK9Ettersendelse()//TODO:Egen søknad for ettersending
                        )
                        CleanupEttersending(
                            metadata = entry.metadata,
                            melding = entry.data,
                            journalførtMelding = journalfort
                        )
                    }
                }
            mapValues
                .to(tilCleanup.name, Produced.with(tilCleanup.keySerde, tilCleanup.valueSerde))
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}

private fun PreprossesertEttersending.tilK9Ettersendelse(): Ettersendelse = Ettersendelse.builder()
    .mottattDato(mottatt)
    .søker(soker.tilK9Søker())
    .ytelse(Ytelse.PLEIEPENGER_SYKT_BARN)
    .build()

private fun PreprossesertSoker.tilK9Søker(): Søker = Søker.builder()
    .norskIdentitetsnummer(NorskIdentitetsnummer.of(fodselsnummer))
    .build()
