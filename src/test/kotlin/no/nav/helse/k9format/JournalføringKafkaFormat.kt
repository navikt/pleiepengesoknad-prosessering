package no.nav.helse.k9format

import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.pleiepengerbarn.PleiepengerBarnSøknad
import org.json.JSONObject
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.assertNotNull


internal fun String.assertJournalførtFormat(printJournalført: Boolean = false): PleiepengerBarnSøknad {
    val logger: Logger = LoggerFactory.getLogger("K9 JournalførtFormat")
    val rawJson = JSONObject(this)

    val metadata = assertNotNull(rawJson.getJSONObject("metadata"))
    assertNotNull(metadata.getString("correlationId"))

    val data = assertNotNull(rawJson.getJSONObject("data"))
    assertNotNull(data.getString("journalpostId"))

    val søknad = assertNotNull(data.getJSONObject("søknad"))

    val rekonstruertSøknad = PleiepengerBarnSøknad
        .builder()
        .json(søknad.toString())
        .build()

    JSONAssert.assertEquals(søknad.toString(), PleiepengerBarnSøknad.SerDes.serialize(rekonstruertSøknad), true)
    if (printJournalført) {
        logger.info(JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(rekonstruertSøknad))
    }
    return rekonstruertSøknad
}
