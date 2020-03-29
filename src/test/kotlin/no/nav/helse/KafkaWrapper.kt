package no.nav.helse

import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.PreprossesertMeldingV1
import no.nav.helse.prosessering.v1.asynkron.TopicEntry
import no.nav.helse.prosessering.v1.asynkron.Topics
import no.nav.helse.prosessering.v1.asynkron.Topics.CLEANUP
import no.nav.helse.prosessering.v1.asynkron.Topics.ETTERSENDING_CLEANUP
import no.nav.helse.prosessering.v1.asynkron.Topics.ETTERSENDING_JOURNALFORT
import no.nav.helse.prosessering.v1.asynkron.Topics.ETTERSENDING_MOTTATT
import no.nav.helse.prosessering.v1.asynkron.Topics.ETTERSENDING_PREPROSSESERT
import no.nav.helse.prosessering.v1.asynkron.Topics.JOURNALFORT
import no.nav.helse.prosessering.v1.asynkron.Topics.MOTTATT
import no.nav.helse.prosessering.v1.asynkron.Topics.PREPROSSESERT
import no.nav.helse.prosessering.v1.ettersending.Ettersending
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.*
import kotlin.test.assertEquals

private const val username = "srvkafkaclient"
private const val password = "kafkaclient"

object KafkaWrapper {
    fun bootstrap() : KafkaEnvironment {
        val kafkaEnvironment = KafkaEnvironment(
            users = listOf(JAASCredential(username, password)),
            autoStart = true,
            withSchemaRegistry = false,
            withSecurity = true,
            topicNames= listOf(
                MOTTATT.name,
                PREPROSSESERT.name,
                JOURNALFORT.name,
                CLEANUP.name,
                ETTERSENDING_MOTTATT.name,
                ETTERSENDING_PREPROSSESERT.name,
                ETTERSENDING_JOURNALFORT.name,
                ETTERSENDING_CLEANUP.name
            )
        )
        return kafkaEnvironment
    }
}

private fun KafkaEnvironment.testConsumerProperties(clientId: String): MutableMap<String, Any>?  {
    return HashMap<String, Any>().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokersURL)
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";")
        put(ConsumerConfig.GROUP_ID_CONFIG, clientId)
    }
}

private fun KafkaEnvironment.testProducerProperties() : MutableMap<String, Any>?  {
    return HashMap<String, Any>().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokersURL)
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";")
        put(ProducerConfig.CLIENT_ID_CONFIG, "PleiepengesoknadProsesseringTestProducer")
    }
}


fun KafkaEnvironment.testConsumer() : KafkaConsumer<String, TopicEntry<PreprossesertMeldingV1>> {
    val consumer = KafkaConsumer<String, TopicEntry<PreprossesertMeldingV1>>(
        testConsumerProperties("PleiepengesoknadProsesseringTestConsumer"),
        StringDeserializer(),
        PREPROSSESERT.serDes
    )
    consumer.subscribe(listOf(PREPROSSESERT.name))
    return consumer
}

fun KafkaEnvironment.journalføringsKonsumer(): KafkaConsumer<String, String> {
    val consumer = KafkaConsumer(
        testConsumerProperties("K9FordelKonsumer"),
        StringDeserializer(),
        StringDeserializer()
    )
    consumer.subscribe(listOf(JOURNALFORT.name))
    return consumer
}

fun KafkaEnvironment.ettersendingJournalføringsKonsumer(): KafkaConsumer<String, String> {
    val consumer = KafkaConsumer(
        testConsumerProperties("K9FordelEttersendelseKonsumer"),
        StringDeserializer(),
        StringDeserializer()
    )
    consumer.subscribe(listOf(ETTERSENDING_JOURNALFORT.name))
    return consumer
}

fun KafkaEnvironment.testProducer() = KafkaProducer(
    testProducerProperties(),
    Topics.MOTTATT.keySerializer,
    Topics.MOTTATT.serDes
)

fun KafkaEnvironment.testEttersendingProducer() = KafkaProducer(
    testProducerProperties(),
    Topics.ETTERSENDING_MOTTATT.keySerializer,
    Topics.ETTERSENDING_MOTTATT.serDes
)

fun KafkaConsumer<String, TopicEntry<PreprossesertMeldingV1>>.hentPreprosessertMelding(
    soknadId: String,
    maxWaitInSeconds: Long = 20
) : TopicEntry<PreprossesertMeldingV1> {
    val end = System.currentTimeMillis() + Duration.ofSeconds(maxWaitInSeconds).toMillis()
    while (System.currentTimeMillis() < end) {
        seekToBeginning(assignment())
        val entries = poll(Duration.ofSeconds(1))
            .records(PREPROSSESERT.name)
            .filter { it.key() == soknadId }

        if (entries.isNotEmpty()) {
            assertEquals(1, entries.size)
            return entries.first().value()
        }
    }
    throw IllegalStateException("Fant ikke preprosessert melding for søknad $soknadId etter $maxWaitInSeconds sekunder.")
}

fun KafkaConsumer<String, String>.hentJournalførtMelding(
    soknadId: String,
    maxWaitInSeconds: Long = 20
): String {
    val end = System.currentTimeMillis() + Duration.ofSeconds(maxWaitInSeconds).toMillis()
    while (System.currentTimeMillis() < end) {
        seekToBeginning(assignment())
        val entries = poll(Duration.ofSeconds(1))
            .records(JOURNALFORT.name)
            .filter { it.key() == soknadId }

        if (entries.isNotEmpty()) {
            assertEquals(1, entries.size)
            return entries.first().value()
        }
    }
    throw IllegalStateException("Fant ikke journalført melding for søknad $soknadId etter $maxWaitInSeconds sekunder.")
}

fun KafkaConsumer<String, String>.hentJournalførtEttersending(
    soknadId: String,
    maxWaitInSeconds: Long = 20
): String {
    val end = System.currentTimeMillis() + Duration.ofSeconds(maxWaitInSeconds).toMillis()
    while (System.currentTimeMillis() < end) {
        seekToBeginning(assignment())
        val entries = poll(Duration.ofSeconds(1))
            .records(ETTERSENDING_JOURNALFORT.name)
            .filter { it.key() == soknadId }

        if (entries.isNotEmpty()) {
            assertEquals(1, entries.size)
            return entries.first().value()
        }
    }
    throw IllegalStateException("Fant ikke journalført ettersending med søknadId $soknadId etter $maxWaitInSeconds sekunder.")
}

fun KafkaProducer<String, TopicEntry<MeldingV1>>.leggSoknadTilProsessering(soknad: MeldingV1) {
    send(
        ProducerRecord(
            Topics.MOTTATT.name,
            soknad.soknadId,
            TopicEntry(
                metadata = Metadata(
                    version = 1,
                    correlationId = UUID.randomUUID().toString(),
                    requestId =  UUID.randomUUID().toString()
                ),
                data = soknad
            )
        )
    ).get()
}

fun KafkaProducer<String, TopicEntry<Ettersending>>.leggEttersendingTilProsessering(ettersending: Ettersending) {
    send(
        ProducerRecord(
            Topics.ETTERSENDING_MOTTATT.name,
            ettersending.søknadId,
            TopicEntry(
                metadata = Metadata(
                    version = 1,
                    correlationId = UUID.randomUUID().toString(),
                    requestId =  UUID.randomUUID().toString()
                ),
                data = ettersending
            )
        )
    ).get()
}

fun KafkaEnvironment.username() = username
fun KafkaEnvironment.password() = password
