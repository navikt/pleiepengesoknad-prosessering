package no.nav.helse

import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.prosessering.v1.asynkron.Topics.JOURNALFORT
import no.nav.helse.prosessering.v1.asynkron.Topics.MOTTATT
import no.nav.helse.prosessering.v1.asynkron.Topics.OPPGAVE_OPPRETTET
import no.nav.helse.prosessering.v1.asynkron.Topics.PREPROSSESERT
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SaslConfigs

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
                OPPGAVE_OPPRETTET.name
            )
        )
        return kafkaEnvironment
    }
}

fun KafkaEnvironment.testConsumerProperties() : MutableMap<String, Any>?  {
    return HashMap<String, Any>().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokersURL)
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";")
        put(ConsumerConfig.GROUP_ID_CONFIG, "PleiepengesoknadProsesseringTest")
    }
}
fun KafkaEnvironment.username() = username
fun KafkaEnvironment.password() = password