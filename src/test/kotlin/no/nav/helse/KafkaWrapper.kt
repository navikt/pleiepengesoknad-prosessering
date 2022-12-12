package no.nav.helse

import no.nav.helse.kafka.TopicEntry
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.v1.asynkron.EndringsmeldingTopics
import no.nav.helse.prosessering.v1.asynkron.SøknadTopics
import no.nav.helse.prosessering.v1.asynkron.Topic
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.*
import kotlin.test.assertEquals

private const val confluentVersion = "7.2.1"
private lateinit var kafkaContainer: KafkaContainer

object KafkaWrapper {
    fun bootstrap(): KafkaContainer {
        kafkaContainer = KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:$confluentVersion")
        )
        kafkaContainer.start()
        kafkaContainer.createTopicsForTest()
        return kafkaContainer
    }
}

private fun KafkaContainer.createTopicsForTest() {
    // Dette er en workaround for att testcontainers (pr. versjon 1.17.5) ikke håndterer autocreate topics
    AdminClient.create(testProducerProperties("admin")).createTopics(
        listOf(
            NewTopic(SøknadTopics.MOTTATT_v2.name, 1, 1),
            NewTopic(SøknadTopics.PREPROSSESERT.name, 1, 1),
            NewTopic(SøknadTopics.CLEANUP.name, 1, 1),
            NewTopic(EndringsmeldingTopics.ENDRINGSMELDING_MOTTATT.name, 1, 1),
            NewTopic(EndringsmeldingTopics.ENDRINGSMELDING_PREPROSSESERT.name, 1, 1),
            NewTopic(EndringsmeldingTopics.ENDRINGSMELDING_CLEANUP.name, 1, 1),
        )
    )
}

private fun KafkaContainer.testConsumerProperties(groupId: String): MutableMap<String, Any>? {
    return HashMap<String, Any>().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
    }
}

private fun KafkaContainer.testProducerProperties(clientId: String): MutableMap<String, Any>? {
    return HashMap<String, Any>().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        put(ProducerConfig.CLIENT_ID_CONFIG, clientId)
    }
}


fun <T> KafkaContainer.testConsumer(topic: Topic<TopicEntry<T>>): KafkaConsumer<String, TopicEntry<T>> {
    val consumer = KafkaConsumer(
        testConsumerProperties("pleiepengesoknad-prosessering-${UUID.randomUUID()}"),
        StringDeserializer(),
        topic.serDes
    )
    consumer.subscribe(listOf(topic.name))
    return consumer
}

fun <T> KafkaContainer.cleanupConsumer(topic: Topic<TopicEntry<T>>, consumerClientId: String): KafkaConsumer<String, String> {
    val consumer = KafkaConsumer(
        testConsumerProperties("$consumerClientId-${UUID.randomUUID()}"),
        StringDeserializer(),
        StringDeserializer()
    )
    consumer.subscribe(listOf(topic.name))
    return consumer
}


fun <T> KafkaContainer.testProducer(producerClientId: String, topic: Topic<TopicEntry<T>>) = KafkaProducer(
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
    maxWaitInSeconds: Long = 60,
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
    topic: Topic<TopicEntry<V>>
) {
    send(
        ProducerRecord(
            topic.name,
            søknadId,
            TopicEntry(
                metadata = Metadata(
                    version = 1,
                    correlationId = UUID.randomUUID().toString()
                ),
                data = soknad
            )
        )
    ).get()
}
