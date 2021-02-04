package no.nav.helse.k9format

import no.nav.helse.felles.Beredskap
import no.nav.helse.felles.Bosted
import no.nav.helse.felles.Ferieuttak
import no.nav.helse.felles.FerieuttakIPerioden
import no.nav.helse.felles.Medlemskap
import no.nav.helse.felles.Nattevåk
import no.nav.helse.felles.Organisasjon
import no.nav.helse.felles.Tilsynsordning
import no.nav.helse.felles.TilsynsordningJa
import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.felles.type.Periode
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.time.Duration
import java.time.LocalDate

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

        JSONAssert.assertEquals(forventetJson, JsonUtils.toString(nattevåkK9Format), true)
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

        JSONAssert.assertEquals(forventetJson, JsonUtils.toString(beredskapK9Format), true)
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
        JSONAssert.assertEquals(forventetJson, JsonUtils.toString(bostederK9Format), true)
    }

    @Test
    fun `Organisasjon til K9ArbeidstidInfo`() {
        val org = Organisasjon(
            organisasjonsnummer = "",
            navn = "",
            skalJobbe = "",
            jobberNormaltTimer = 35.5,
            skalJobbeProsent = 50.0
        )

        val k9ArbeidstidInfo = org.tilK9ArbeidstidInfo(periode)

        val forventetNormaltimerPerDag = org.jobberNormaltTimer.tilTimerPerDag().tilDuration()
        val forventetFaktiskArbeidstimerPerDag =
            org.jobberNormaltTimer.tilFaktiskTimerPerUke(org.skalJobbeProsent).tilTimerPerDag().tilDuration()

        val forventetJson =
            //language=json
            """
            {
              "jobberNormaltTimerPerDag" : "$forventetNormaltimerPerDag",
              "perioder" : {
                "2020-01-01/2020-01-31" : {
                  "faktiskArbeidTimerPerDag" : "$forventetFaktiskArbeidstimerPerDag"
                }
              }
            }
        """.trimIndent()

        JSONAssert.assertEquals(forventetJson, JsonUtils.toString(k9ArbeidstidInfo), true)
    }

    @Test
    fun `FerieuttakIPerioden tilK9LovbestemtFerie`() {
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

        JSONAssert.assertEquals(forventetJson, JsonUtils.toString(k9LovbestemtFerie), true)
    }

    @Test
    fun `Tilsynsordning til k9Tilsynsordning`() {
        val tilsynsordning = Tilsynsordning(
            svar = "ja",
            ja = TilsynsordningJa(
                mandag = Duration.parse("PT7H30M"),
                tirsdag = Duration.parse("PT4H30M"),
                onsdag = Duration.parse("PT2H30M"),
                torsdag = null,
                fredag = Duration.parse("PT5H")
            ),
            vetIkke = null
        ).tilK9Tilsynsordning(periode)

        val forventetJson =
            //language=json
            """
                {
                  "perioder" : {
                    "2020-01-01/2020-01-31" : {
                      "etablertTilsynTimerPerDag" : "PT3H54M"
                    }
                  }
                }
            """.trimIndent()

        JSONAssert.assertEquals(forventetJson, JsonUtils.toString(tilsynsordning), true)
    }
}
