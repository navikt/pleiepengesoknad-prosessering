package no.nav.helse.k9mellomlagring

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.k9.søknad.Søknad

class JournalforingsFormat {
    companion object {
        private val objectMapper = jacksonObjectMapper()
            .dusseldorfConfigured()
            .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
            .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)

        internal fun somJson(
            k9FormatSøknad: Søknad
        ): ByteArray {
            val node = objectMapper.valueToTree<ObjectNode>(k9FormatSøknad)
            node.remove("vedlegg_urls")
            return objectMapper.writeValueAsBytes(node)
        }
    }
}
