package no.nav.helse.dokument

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.prosessering.v1.MeldingV1

class JournalforingsFormat {
    companion object {
        private val objectMapper = jacksonObjectMapper()
            .dusseldorfConfigured()
            .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)

        internal fun somJson(
            meldingV1: MeldingV1
        ): ByteArray {
            val node = objectMapper.valueToTree<ObjectNode>(meldingV1)
            node.remove("vedlegg_urls")
            return objectMapper.writeValueAsBytes(node)
        }
    }
}