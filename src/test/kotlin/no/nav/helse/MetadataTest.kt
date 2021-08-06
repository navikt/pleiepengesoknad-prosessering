package no.nav.helse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.prosessering.Metadata
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.Test
import kotlin.test.assertEquals

internal class MetadataTest {

    private companion object {
        private val mapper = jacksonObjectMapper()
            .dusseldorfConfigured()

        private val reserialisert = """
            {
                "version": 1,
                "correlationId": "foo"
            }
            """.trimIndent()
    }

    @Test
    internal fun `Deserialisering og serialisering fra camelCase`() {
        val metadata: Metadata = mapper.readValue(reserialisert)
        assertEquals(1, metadata.version)
        assertEquals("foo", metadata.correlationId)

        JSONAssert.assertEquals(reserialisert, mapper.writeValueAsString(metadata), true)
    }
}
