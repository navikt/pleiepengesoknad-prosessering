package no.nav.helse.dokument

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.prosessering.SoknadId
import no.nav.helse.prosessering.v1.MeldingV1

class JournalforingsFormat {
    companion object {
        private val objectMapper = jacksonObjectMapper().dusseldorfConfigured()

        internal fun somJson(
            meldingV1: MeldingV1,
            soknadId: SoknadId
        ): ByteArray {
            val node = objectMapper.valueToTree<ObjectNode>(meldingV1)
            node.put("soknad_id", soknadId.id)
            node.remove("vedlegg")
            node.remove("vedlegg_urls")
            return objectMapper.writeValueAsBytes(node)
        }
    }
}