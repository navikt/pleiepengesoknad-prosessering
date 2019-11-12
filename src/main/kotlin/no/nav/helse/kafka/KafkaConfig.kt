package no.nav.helse.kafka

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.streams.StreamsConfig.*
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Duration
import java.util.*

private val logger: Logger = LoggerFactory.getLogger(KafkaConfig::class.java)
private const val ID_PREFIX = "srvpps-prosessering-"

internal class KafkaConfig(
    bootstrapServers: String,
    credentials: Pair<String, String>,
    trustStore: Pair<String, String>?,
    exactlyOnce: Boolean,
    internal val unreadyAfterStreamStoppedIn: Duration
) {
    private val streams = Properties().apply {
        put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        put(DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler::class.java)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        medCredentials(credentials)
        medTrustStore(trustStore)
        medProcessingGuarantee(exactlyOnce)
    }

    internal fun stream(name: String) = streams.apply {
        put(APPLICATION_ID_CONFIG, "$ID_PREFIX$name")
    }
}

private fun Properties.medProcessingGuarantee(exactlyOnce: Boolean) {
    if (exactlyOnce) {
        logger.info("$PROCESSING_GUARANTEE_CONFIG=$EXACTLY_ONCE")
        put(PROCESSING_GUARANTEE_CONFIG, EXACTLY_ONCE)
        put(REPLICATION_FACTOR_CONFIG, "3")
    } else {
        logger.info("$PROCESSING_GUARANTEE_CONFIG=$AT_LEAST_ONCE")
        put(PROCESSING_GUARANTEE_CONFIG, AT_LEAST_ONCE)
    }
}

private fun Properties.medTrustStore(trustStore: Pair<String, String>?) {
    trustStore?.let {
        try {
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(it.first).absolutePath)
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, it.second)
            logger.info("Truststore på '${it.first}' konfigurert.")
        } catch (cause: Throwable) {
            logger.error(
                "Feilet for konfigurering av truststore på '${it.first}'",
                cause
            )
        }
    }
}
private fun Properties.medCredentials(credentials: Pair<String, String>) {
    put(SaslConfigs.SASL_MECHANISM, "PLAIN")
    put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
    put(
        SaslConfigs.SASL_JAAS_CONFIG,
        "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${credentials.first}\" password=\"${credentials.second}\";"
    )
}
