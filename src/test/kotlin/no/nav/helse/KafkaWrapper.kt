package no.nav.helse

import org.testcontainers.containers.KafkaContainer

object KafkaWrapper {
    fun bootstrap() : KafkaContainer {
        val kafkaContainer = KafkaContainer("4.0.0")
        return kafkaContainer
    }
}