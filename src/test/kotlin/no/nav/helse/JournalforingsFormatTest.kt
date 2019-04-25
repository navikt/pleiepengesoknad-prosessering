package no.nav.helse

import no.nav.helse.dokument.JournalforingsFormat
import no.nav.helse.dusseldorf.ktor.core.fromResources
import no.nav.helse.prosessering.v1.*
import org.skyscreamer.jsonassert.JSONAssert
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test

class JournalforingsFormatTest {

    @Test
    fun `Soknaden journalfoeres som JSON uten vedlegg`() {
        val json = JournalforingsFormat.somJson(melding())
        JSONAssert.assertEquals("""
        {
            "mottatt": "2018-01-02T03:04:05.000000006Z",
            "fra_og_med": "2018-01-01",
            "til_og_med": "2018-02-02",
            "soker": {
                "fodselsnummer": "1212",
                "fornavn": "Ola",
                "mellomnavn": "Mellomnavn",
                "etternavn": "Nordmann"
            },
            "barn": {
                "fodselsnummer": "2323",
                "navn": "Kari",
                "alternativ_id": null
            },
            "relasjon_til_barnet": "Mor",
            "arbeidsgivere": {
                "organisasjoner": [{
                    "organisasjonsnummer": "1212",
                    "navn": "Nei"
                }]
            },
            "medlemskap": {
                "har_bodd_i_utlandet_siste_12_mnd": true,
                "skal_bo_i_utlandet_neste_12_mnd": true
            },
            "grad": 55,
            "har_medsoker": true,
            "er_selvstendig_naeringsdrivende_eller_frilanser": false,
            "forventes_at_barnet_kan_vaere_i_etablert_tilsynsordning": false
        }
        """.trimIndent(), String(json), true)

    }

    private fun melding() : MeldingV1 = MeldingV1(
        mottatt = ZonedDateTime.of(2018,1,2,3,4,5,6, ZoneId.of("UTC")),
        fraOgMed = LocalDate.parse("2018-01-01"),
        tilOgMed = LocalDate.parse("2018-02-02"),
        soker = Soker(
            fodselsnummer = "1212",
            etternavn = "Nordmann",
            mellomnavn = "Mellomnavn",
            fornavn = "Ola"
        ),
        barn = Barn(
            navn = "Kari",
            fodselsnummer = "2323",
            alternativId = null
        ),
        relasjonTilBarnet = "Mor",
        arbeidsgivere = Arbeidsgivere(
            organisasjoner = listOf(
                Organisasjon("1212", "Nei")
            )
        ),
        vedleggUrls = listOf(
            URL("http://localhost:8080/1234"),
            URL("http://localhost:8080/12345")
        ),
        vedlegg = listOf(
            Vedlegg(
                content = "test.pdf".fromResources().readBytes(),
                contentType = "application/pdf",
                title = "PDF"
            )
        ),
        medlemskap = Medlemskap(
            harBoddIUtlandetSiste12Mnd = true,
            skalBoIUtlandetNeste12Mnd = true
        ),
        harMedsoker = true,
        erSelvstendigNaeringsdrivendeEllerFrilanser = false,
        forventesAtBarnetKanVaereIEtablertTilsynsordning = false,
        grad = 55
    )
}
