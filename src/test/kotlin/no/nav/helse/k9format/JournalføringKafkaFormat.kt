package no.nav.helse.k9format

import no.nav.k9.ettersendelse.Ettersendelse
import no.nav.k9.søknad.pleiepengerbarn.PleiepengerBarnSøknad
import org.json.JSONObject
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.assertNotNull

internal fun String.assertJournalførtFormat() {
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
}

internal fun String.assertEttersendingJournalførtFormat() {
    val rawJson = JSONObject(this)

    val metadata = assertNotNull(rawJson.getJSONObject("metadata"))
    assertNotNull(metadata.getString("correlationId"))

    val data = assertNotNull(rawJson.getJSONObject("data"))
    assertNotNull(data.getString("journalpostId"))

    val søknad = assertNotNull(data.getJSONObject("søknad"))

    val rekonstruertSøknad = Ettersendelse
        .builder()
        .json(søknad.toString())
        .build()

    JSONAssert.assertEquals(søknad.toString(), Ettersendelse.SerDes.serialize(rekonstruertSøknad), true)
}
