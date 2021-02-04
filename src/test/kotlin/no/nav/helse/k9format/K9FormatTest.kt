package no.nav.helse.k9format

import no.nav.helse.felles.*
import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.felles.type.Periode
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import kotlin.test.assertEquals

class K9FormatTest {
    companion object {
        val periode = Periode(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-31"))
    }

    @Test
    fun `Bygge nattevåk til forventet K9Format`() {
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
    fun `Bygge beredskap til forventet K9Format`() {
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
    fun `Bygge Bosteder-K9 fra Medlemskap til forventet K9Format`() {
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
        assertEquals(forventetJson, JsonUtils.toString(bostederK9Format))
    }

    @Test
    fun `Organisasjon til K9ArbeidstidInfo`() {
        val org = Organisasjon(
            organisasjonsnummer = "",
            navn = "",
            skalJobbe = "",
            jobberNormaltTimer = 40.0,
            skalJobbeProsent = 50.0
        )

        val k9ArbeidstidInfo = org.tilK9ArbeidstidInfo(periode)

        val forventetNormaltimerPerDag = org.jobberNormaltTimer.tilTimerPerDag().toLong()
        val forventetFaktiskArbeidstimerPerDag = org.jobberNormaltTimer.tilFaktiskTimerPerUke(org.skalJobbeProsent).tilTimerPerDag().toLong()

        val forventetJson =
            //language=json
            """
            {
              "jobberNormaltTimerPerDag" : "${Duration.ofHours(forventetNormaltimerPerDag)}",
              "perioder" : {
                "2020-01-01/2020-01-31" : {
                  "faktiskArbeidTimerPerDag" : "${Duration.ofHours(forventetFaktiskArbeidstimerPerDag)}"
                }
              }
            }
        """.trimIndent()

        assertEquals(forventetJson, JsonUtils.toString(k9ArbeidstidInfo))
    }

    @Test
    fun `FerieuttakIPerioden tilK9LovbestemtFerie`(){
        val ferieuttakIPerioden = FerieuttakIPerioden(
            skalTaUtFerieIPerioden = true,
            ferieuttak = listOf(
                Ferieuttak(
                    fraOgMed = LocalDate.parse("2020-01-01"),
                    tilOgMed = LocalDate.parse("2020-01-31"),
                ),
                Ferieuttak(
                    fraOgMed = LocalDate.parse("2020-02-01"),
                    tilOgMed = LocalDate.parse("2020-02-11"),
                )
            )
        )

        val k9LovbestemtFerie = ferieuttakIPerioden.tilK9LovbestemtFerie()

        val forventetJson = """ 
            {
              "perioder" : [ "2020-01-01/2020-01-31", "2020-02-01/2020-02-11" ]
            }
        """.trimIndent()

        assertEquals(forventetJson, JsonUtils.toString(k9LovbestemtFerie))
    }

}
