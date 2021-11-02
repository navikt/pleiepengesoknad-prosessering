package no.nav.helse

import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.kafka.TopicEntry
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.v1.asynkron.EndringsmeldingTopics.ENDRINGSMELDING_CLEANUP
import no.nav.helse.prosessering.v1.asynkron.EndringsmeldingTopics.ENDRINGSMELDING_MOTTATT
import no.nav.helse.prosessering.v1.asynkron.EndringsmeldingTopics.ENDRINGSMELDING_PREPROSSESERT
import no.nav.helse.prosessering.v1.asynkron.SøknadTopics.CLEANUP
import no.nav.helse.prosessering.v1.asynkron.SøknadTopics.MOTTATT
import no.nav.helse.prosessering.v1.asynkron.SøknadTopics.PREPROSSESERT
import no.nav.helse.prosessering.v1.asynkron.Topic
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
    fun bootstrap(): KafkaEnvironment {
        val kafkaEnvironment = KafkaEnvironment(
            users = listOf(JAASCredential(username, password)),
            autoStart = true,
            withSchemaRegistry = false,
            withSecurity = true,
            topicNames = listOf(
                MOTTATT.name,
                PREPROSSESERT.name,
                CLEANUP.name,
                ENDRINGSMELDING_MOTTATT.name,
                ENDRINGSMELDING_PREPROSSESERT.name,
                ENDRINGSMELDING_CLEANUP.name,
            )
        )
        return kafkaEnvironment
    }
}

private fun KafkaEnvironment.testConsumerProperties(clientId: String): MutableMap<String, Any>? {
    return HashMap<String, Any>().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokersURL)
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        put(
            SaslConfigs.SASL_JAAS_CONFIG,
            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";"
        )
        put(ConsumerConfig.GROUP_ID_CONFIG, clientId)
    }
}

private fun KafkaEnvironment.testProducerProperties(producerClientId: String): MutableMap<String, Any>? {
    return HashMap<String, Any>().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokersURL)
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        put(
            SaslConfigs.SASL_JAAS_CONFIG,
            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";"
        )
        put(ProducerConfig.CLIENT_ID_CONFIG, producerClientId)
    }
}


fun <T> KafkaEnvironment.testConsumer(topic: Topic<TopicEntry<T>>): KafkaConsumer<String, TopicEntry<T>> {
    val consumer = KafkaConsumer(
        testConsumerProperties("pleiepengesoknad-prosessering-${UUID.randomUUID()}"),
        StringDeserializer(),
        topic.serDes
    )
    consumer.subscribe(listOf(topic.name))
    return consumer
}

fun <T> KafkaEnvironment.cleanupConsumer(topic: Topic<TopicEntry<T>>, consumerClientId: String): KafkaConsumer<String, String> {
    val consumer = KafkaConsumer(
        testConsumerProperties("$consumerClientId-${UUID.randomUUID()}"),
        StringDeserializer(),
        StringDeserializer()
    )
    consumer.subscribe(listOf(topic.name))
    return consumer
}


fun <T> KafkaEnvironment.testProducer(producerClientId: String, topic: Topic<TopicEntry<T>>) = KafkaProducer(
    testProducerProperties(producerClientId),
    topic.keySerializer,
    topic.serDes
)

fun <V> KafkaConsumer<String, TopicEntry<V>>.hentMelding(
    soknadId: String,
    maxWaitInSeconds: Long = 20,
    topic: Topic<TopicEntry<V>>
): TopicEntry<V> {
    val end = System.currentTimeMillis() + Duration.ofSeconds(maxWaitInSeconds).toMillis()
    while (System.currentTimeMillis() < end) {
        seekToBeginning(assignment())
        val entries = poll(Duration.ofSeconds(1))
            .records(topic.name)
            .filter { it.key() == soknadId }

        if (entries.isNotEmpty()) {
            assertEquals(1, entries.size)
            return entries.first().value()
        }
    }
    throw IllegalStateException("Fant ikke preprosessert melding for søknad $soknadId etter $maxWaitInSeconds sekunder.")
}

fun <V> KafkaConsumer<String, String>.hentCleanupMelding(
    soknadId: String,
    maxWaitInSeconds: Long = 20,
    topic: Topic<TopicEntry<V>>
): String {
    val end = System.currentTimeMillis() + Duration.ofSeconds(maxWaitInSeconds).toMillis()
    while (System.currentTimeMillis() < end) {
        seekToBeginning(assignment())
        val entries = poll(Duration.ofSeconds(1))
            .records(topic.name)
            .filter { it.key() == soknadId }

        if (entries.isNotEmpty()) {
            assertEquals(1, entries.size)
            return entries.first().value()
        }
    }
    throw IllegalStateException("Fant ikke journalført melding for søknad $soknadId etter $maxWaitInSeconds sekunder.")
}

fun <V> KafkaProducer<String, TopicEntry<V>>.leggPåMelding(
    søknadId: String,
    soknad: V,
    topic: Topic<TopicEntry<V>>,
    version: Int = 1
) {
    send(
        ProducerRecord(
            topic.name,
            søknadId,
            TopicEntry(
                metadata = Metadata(
                    version = version,
                    correlationId = UUID.randomUUID().toString()
                ),
                data = soknad
            )
        )
    ).get()
}

fun KafkaEnvironment.username() = username
fun KafkaEnvironment.password() = password
