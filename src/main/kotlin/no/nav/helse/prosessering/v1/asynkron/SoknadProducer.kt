package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.SoknadId
import no.nav.helse.prosessering.v1.MeldingV1
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

internal class SoknadProducer(
    kafkaConfig: KafkaConfig
) {
    private companion object {
        private val NAME = "SoknadProducerV1"
        private val logger = LoggerFactory.getLogger(SoknadProducer::class.java)
    }

    private val producer = KafkaProducer<String, TopicEntry<MeldingV1>>(
        kafkaConfig.producer(NAME),
        Topics.MOTTATT.keySerializer,
        Topics.MOTTATT.serDes
    )

    internal fun produce(
        soknadId: SoknadId,
        melding: MeldingV1,
        metadata: Metadata
    ) {
        val recordMetaData = producer.send(ProducerRecord(
            Topics.MOTTATT.name,
            soknadId.id,
            TopicEntry(
                metadata = metadata,
                data = melding
            )
        )).get()

        logger.info("Søknad '${soknadId.id}' sendt til Topic '${Topics.MOTTATT.name}' med offset '${recordMetaData.offset()}' til partition '${recordMetaData.partition()}'")

    }
}