package no.nav.helse.prosessering.v1.kafka

import no.nav.helse.prosessering.v1.MeldingV1
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class KafkaProducerV1 {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger("nav.KafkaProducerV1")

    }
    suspend internal fun produce(
        melding: MeldingV1

    ) {
        logger.info("$melding")
    }
}