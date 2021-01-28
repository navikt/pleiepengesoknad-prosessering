package no.nav.helse.k9format

import no.nav.helse.prosessering.v1.Beredskap
import no.nav.helse.prosessering.v1.Bosted
import no.nav.helse.prosessering.v1.Medlemskap
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

    @Test
    fun `Bygge Bosteder-K9 fra Medlemskap til forventet K9Format`(){
        val medlemskap = Medlemskap(
            harBoddIUtlandetSiste12Mnd = true,
            utenlandsoppholdSiste12Mnd = listOf(
                Bosted(
                    LocalDate.of(2020, 1, 2),
                    LocalDate.of(2020, 1, 3),
                    "US", "USA"
                ),
                Bosted(
                    LocalDate.of(2020, 6, 1),
                    LocalDate.of(2020, 6, 23),
                    "DEU", "Tyskland"
                )
            ),
            skalBoIUtlandetNeste12Mnd = true,
            utenlandsoppholdNeste12Mnd = listOf(
                Bosted(
                    LocalDate.of(2021, 2, 2),
                    LocalDate.of(2021, 2, 3),
                    "US", "USA"
                ),
                Bosted(
                    LocalDate.of(2021, 6, 1),
                    LocalDate.of(2021, 6, 23),
                    "DEU", "Tyskland"
                )
            )
        )
        val bostederK9Format = medlemskap.tilK9Bosteder()
        val forventetJson = """
            {
              "perioder" : {
                "2021-06-01/2021-06-23" : {
                  "land" : "DEU"
                },
                "2020-06-01/2020-06-23" : {
                  "land" : "DEU"
                },
                "2020-01-02/2020-01-03" : {
                  "land" : "US"
                },
                "2021-02-02/2021-02-03" : {
                  "land" : "US"
                }
              }
            }
        """.trimIndent()
        println(JsonUtils.toString(bostederK9Format))
        assertEquals(forventetJson, JsonUtils.toString(bostederK9Format))
    }

}