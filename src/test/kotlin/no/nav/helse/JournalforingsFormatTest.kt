package no.nav.helse

import no.nav.helse.dokument.JournalforingsFormat
import org.skyscreamer.jsonassert.JSONAssert
import java.util.*
import kotlin.test.Test

class JournalforingsFormatTest {

    @Test
    fun `Søknaden journalfoeres som JSON uten vedlegg`() {
        val soknadId = UUID.randomUUID()
        val json = JournalforingsFormat.somJson(SøknadUtils.defaultK9FormatPSB(søknadId = soknadId))
        println(String(json))
        JSONAssert.assertEquals(
            //language=json
            """
        {
          "søknadId": "$soknadId",
          "journalposter": [],
          "versjon": "1.0.0",
          "språk": "nb",
          "mottattDato": "2020-01-01T10:00:00.000Z",
          "søker": {
            "norskIdentitetsnummer": "12345678910"
          },
          "begrunnelseForInnsending": {
            "tekst": null
          },
          "ytelse": {
            "type": "PLEIEPENGER_SYKT_BARN",
            "søknadsperiode": ["2020-01-01/2020-01-10"],
            "endringsperiode": [],
            "trekkKravPerioder": [],
            "infoFraPunsj": null,
            "dataBruktTilUtledning" : {
              "harForståttRettigheterOgPlikter" : true,
              "harBekreftetOpplysninger" : true,
              "samtidigHjemme" : true,
              "harMedsøker" : true,
              "bekrefterPeriodeOver8Uker" : true
            },
            "barn": {
              "norskIdentitetsnummer": "10987654321",
              "fødselsdato": null
            },
            "opptjeningAktivitet": {
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
                "sluttdato": null
              }
            },
            "beredskap": {
              "perioderSomSkalSlettes": {},
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
              "perioderSomSkalSlettes": {},
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
                    "perioder": {
                      "2018-01-01/2020-01-05": {
                         "jobberNormaltTimerPerDag": "PT8H",
                        "faktiskArbeidTimerPerDag": "PT4H"
                      },
                      "2020-01-06/2020-01-10": {
                         "jobberNormaltTimerPerDag": "PT8H",
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
            "omsorg" : {
              "relasjonTilBarnet" : "MOR",
              "beskrivelseAvOmsorgsrollen" : "Blabla beskrivelse"
            },
            "lovbestemtFerie": {
              "perioder": {
                "2020-01-01/2020-01-05":  {
                  "skalHaFerie" : true
                },
                "2020-01-06/2020-01-10": {
                  "skalHaFerie" : true
                }
              }
            },
            "bosteder": {
              "perioderSomSkalSlettes": {},
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
              },
              "perioderSomSkalSlettes": {}
            }
          }
        }
        """.trimIndent(), String(json), true
        )

    }
}
