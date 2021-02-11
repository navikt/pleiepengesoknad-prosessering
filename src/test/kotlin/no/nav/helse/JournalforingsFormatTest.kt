package no.nav.helse

import no.nav.helse.dokument.JournalforingsFormat
import no.nav.helse.prosessering.v1.*
import org.skyscreamer.jsonassert.JSONAssert
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.Test

class JournalforingsFormatTest {

    @Test
    fun `Soknaden journalfoeres som JSON uten vedlegg`() {
        val soknadId = UUID.randomUUID()
        val json = JournalforingsFormat.somJson(SøknadUtils.defaultK9FormatPSB(søknadId = soknadId))
        println(String(json))
        JSONAssert.assertEquals(
            //language=json
            """
        {
          "søknadId": "$soknadId",
          "versjon": "1.0",
          "språk": "nb",
          "mottattDato": "2020-01-01T10:00:00.000Z",
          "søker": {
            "norskIdentitetsnummer": "12345678910"
          },
          "ytelse": {
            "type": "PLEIEPENGER_SYKT_BARN",
            "søknadsperiode": "2020-01-01/2020-01-10",
            "søknadInfo": {
              "relasjonTilBarnet": "Far",
              "samtykketOmsorgForBarnet": true,
              "beskrivelseAvOmsorgsrollen": "beskriver omsorgsrollen...",
              "harForståttRettigheterOgPlikter": true,
              "harBekreftetOpplysninger": true,
              "flereOmsorgspersoner": true,
              "samtidigHjemme": true,
              "harMedsøker": true,
              "bekrefterPeriodeOver8Uker": true
            },
            "barn": {
              "norskIdentitetsnummer": "10987654321",
              "fødselsdato": null
            },
            "arbeidAktivitet": {
              "selvstendigNæringsdrivende": [
                {
                  "perioder": {
                    "2018-01-01/2020-01-01": {
                      "virksomhetstyper": [
                        "DAGMAMMA",
                        "ANNEN"
                      ],
                      "regnskapsførerNavn": "Regnskapsfører Svensen",
                      "regnskapsførerTlf": "+4799887766",
                      "erVarigEndring": true,
                      "endringDato": "2020-01-01",
                      "endringBegrunnelse": "Grunnet Covid-19",
                      "bruttoInntekt": 5000000,
                      "erNyoppstartet": true,
                      "registrertIUtlandet": false,
                      "landkode": "NOR"
                    }
                  },
                  "organisasjonsnummer": "12345678910112233444455667",
                  "virksomhetNavn": "Mamsen Bamsen AS"
                },
                {
                  "perioder": {
                    "2015-01-01/2017-01-01": {
                      "virksomhetstyper": [
                        "FISKE"
                      ],
                      "erVarigEndring": false,
                      "bruttoInntekt": 500000,
                      "erNyoppstartet": false,
                      "registrertIUtlandet": true,
                      "landkode": "ESP"
                    }
                  },
                  "organisasjonsnummer": "54549049090490498048940940",
                  "virksomhetNavn": "Something Fishy AS"
                }
              ],
              "frilanser": {
                "startdato": "2020-01-01",
                "jobberFortsattSomFrilans": true
              }
            },
            "beredskap": {
              "perioder": {
                "2020-01-01/2020-01-05": {
                  "tilleggsinformasjon": "Jeg skal være i beredskap. Basta!"
                },
                "2020-01-07/2020-01-10": {
                  "tilleggsinformasjon": "Jeg skal være i beredskap i denne perioden også. Basta!"
                }
              }
            },
            "nattevåk": {
              "perioder": {
                "2020-01-01/2020-01-05": {
                  "tilleggsinformasjon": "Jeg skal ha nattevåk. Basta!"
                },
                "2020-01-07/2020-01-10": {
                  "tilleggsinformasjon": "Jeg skal ha nattevåk i perioden også. Basta!"
                }
              }
            },
            "tilsynsordning": {
              "perioder": {
                "2020-01-01/2020-01-05": {
                  "etablertTilsynTimerPerDag": "PT8H"
                },
                "2020-01-06/2020-01-10": {
                  "etablertTilsynTimerPerDag": "PT4H"
                }
              }
            },
            "arbeidstid": {
              "arbeidstakerList": [
                {
                  "norskIdentitetsnummer": "12345678910",
                  "organisasjonsnummer": "926032925",
                  "arbeidstidInfo": {
                    "jobberNormaltTimerPerDag": "PT8H",
                    "perioder": {
                      "2018-01-01/2020-01-05": {
                        "faktiskArbeidTimerPerDag": "PT4H"
                      },
                      "2020-01-06/2020-01-10": {
                        "faktiskArbeidTimerPerDag": "PT2H"
                      }
                    }
                  }
                }
              ],
              "frilanserArbeidstidInfo": null,
              "selvstendigNæringsdrivendeArbeidstidInfo": null
            },
            "uttak": {
              "perioder": {
                "2020-01-01/2020-01-05": {
                  "timerPleieAvBarnetPerDag": "PT4H"
                },
                "2020-01-06/2020-01-10": {
                  "timerPleieAvBarnetPerDag": "PT2H"
                }
              }
            },
            "lovbestemtFerie": {
              "perioder": [
                "2020-01-01/2020-01-05",
                "2020-01-06/2020-01-10"
              ]
            },
            "bosteder": {
              "perioder": {
                "2020-01-01/2020-01-05": {
                  "land": "ESP"
                },
                "2020-01-06/2020-01-10": {
                  "land": "NOR"
                }
              }
            },
            "utenlandsopphold": {
              "perioder": {
                "2020-01-01/2020-01-05": {
                  "land": "CAN",
                  "årsak": "barnetInnlagtIHelseinstitusjonDekketEtterAvtaleMedEtAnnetLandOmTrygd"
                },
                "2020-01-06/2020-01-10": {
                  "land": "SWE",
                  "årsak": "barnetInnlagtIHelseinstitusjonForNorskOffentligRegning"
                }
              }
            }
          }
        }
        """.trimIndent(), String(json), true
        )

    }
}
