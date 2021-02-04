package no.nav.helse.k9format

import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import org.json.JSONObject
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.assertNotNull


internal fun String.assertJournalførtFormat(printJournalført: Boolean = false): Søknad {
    val logger: Logger = LoggerFactory.getLogger("K9 JournalførtFormat")
    val rawJson = JSONObject(this)

    val metadata = assertNotNull(rawJson.getJSONObject("metadata"))
    assertNotNull(metadata.getString("correlationId"))

    val data = assertNotNull(rawJson.getJSONObject("data"))
    assertNotNull(data.getString("journalpostId"))

    val søknad = assertNotNull(data.getJSONObject("søknad"))

    val rekonstruertSøknad = JsonUtils.fromString(søknad.toString(), Søknad::class.java)

    val rekonstruertSøknadSomString = JsonUtils.toString(rekonstruertSøknad)
    JSONAssert.assertEquals(søknad.toString(), rekonstruertSøknadSomString, true)
    if (printJournalført) {
        logger.info(rekonstruertSøknadSomString)
    }

    return rekonstruertSøknad
}
