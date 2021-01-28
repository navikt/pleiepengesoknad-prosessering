package no.nav.helse.k9format

import no.nav.helse.prosessering.v1.Beredskap
import no.nav.helse.prosessering.v1.Nattevåk
import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.felles.type.Periode
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class K9FormatTest {
    companion object{
        val periode = Periode(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-31"))
    }

    @Test
    fun `Bygge nattevåk til forventet K9Format`(){
        val nattevåk = Nattevåk(
            harNattevåk = true,
            tilleggsinformasjon = "Barnet sover ikke"
        )
        val nattevåkK9Format = nattevåk.tilK9Nattevåk(periode)
        val forventetJson = """
            {
              "perioder" : {
                "2020-01-01/2020-01-31" : {
                  "tilleggsinformasjon" : "Barnet sover ikke"
                }
              }
            }
        """.trimIndent()

        assertEquals(forventetJson, JsonUtils.toString(nattevåkK9Format))
    }

    @Test
    fun `Bygge beredskap til forventet K9Format`(){
        val beredskap = Beredskap(
            beredskap = true,
            tilleggsinformasjon = "Må være beredt"
        )
        val beredskapK9Format = beredskap.tilK9Beredskap(periode)
        val forventetJson = """
            {
              "perioder" : {
                "2020-01-01/2020-01-31" : {
                  "tilleggsinformasjon" : "Må være beredt"
                }
              }
            }
        """.trimIndent()

        assertEquals(forventetJson, JsonUtils.toString(beredskapK9Format))
    }
}