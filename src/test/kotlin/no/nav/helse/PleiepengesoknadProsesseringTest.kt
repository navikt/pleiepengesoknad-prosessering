package no.nav.helse

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.typesafe.config.ConfigFactory
import io.ktor.config.ApplicationConfig
import io.ktor.config.HoconApplicationConfig
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.createTestEnvironment
import io.ktor.server.testing.handleRequest
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import no.nav.common.KafkaEnvironment
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.dusseldorf.ktor.testsupport.wiremock.WireMockBuilder
import no.nav.helse.prosessering.v1.*
import no.nav.helse.prosessering.v1.asynkron.Journalfort
import no.nav.helse.prosessering.v1.asynkron.TopicEntry
import org.junit.AfterClass
import org.junit.BeforeClass
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


@KtorExperimentalAPI
class PleiepengesoknadProsesseringTest {

    @KtorExperimentalAPI
    private companion object {

        private val logger: Logger = LoggerFactory.getLogger(PleiepengesoknadProsesseringTest::class.java)

        private val wireMockServer: WireMockServer = WireMockBuilder()
            .withNaisStsSupport()
            .withAzureSupport()
            .navnOppslagConfig()
            .build()
            .stubK9DokumentHealth()
            .stubPleiepengerJoarkHealth()
            .stubJournalfor()
            .stubLagreDokument()
            .stubSlettDokument()
            .stubAktoerRegister("29099012345", "123456")

        private val kafkaEnvironment = KafkaWrapper.bootstrap()
        private val kafkaTestConsumer = kafkaEnvironment.testConsumer()
        private val journalførtConsumer = kafkaEnvironment.journalføringsKonsumer()
        private val kafkaTestProducer = kafkaEnvironment.testProducer()

        // Se https://github.com/navikt/dusseldorf-ktor#f%C3%B8dselsnummer
        private val gyldigFodselsnummerA = "02119970078"
        private val gyldigFodselsnummerB = "19066672169"
        private val gyldigFodselsnummerC = "20037473937"
        private val dNummerA = "55125314561"

        private var engine = newEngine(kafkaEnvironment).apply {
            start(wait = true)
        }

        private fun getConfig(kafkaEnvironment: KafkaEnvironment?): ApplicationConfig {
            val fileConfig = ConfigFactory.load()
            val testConfig = ConfigFactory.parseMap(
                TestConfiguration.asMap(
                    wireMockServer = wireMockServer,
                    kafkaEnvironment = kafkaEnvironment
                )
            )
            val mergedConfig = testConfig.withFallback(fileConfig)
            return HoconApplicationConfig(mergedConfig)
        }

        private fun newEngine(kafkaEnvironment: KafkaEnvironment?) = TestApplicationEngine(createTestEnvironment {
            config = getConfig(kafkaEnvironment)
        })

        private fun stopEngine() = engine.stop(5, 60, TimeUnit.SECONDS)

        internal fun restartEngine() {
            stopEngine()
            engine = newEngine(kafkaEnvironment)
            engine.start(wait = true)
        }

        @BeforeClass
        @JvmStatic
        fun buildUp() {
            wireMockServer.stubAktoerRegister(gyldigFodselsnummerA, "666666666")
            wireMockServer.stubAktoerRegister(gyldigFodselsnummerB, "777777777")
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            logger.info("Tearing down")
            wireMockServer.stop()
            kafkaTestConsumer.close()
            kafkaTestProducer.close()
            stopEngine()
            kafkaEnvironment.tearDown()
            logger.info("Tear down complete")
        }
    }

    @Test
    fun `test isready, isalive, health og metrics`() {
        with(engine) {
            handleRequest(HttpMethod.Get, "/isready") {}.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                handleRequest(HttpMethod.Get, "/isalive") {}.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    handleRequest(HttpMethod.Get, "/metrics") {}.apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        handleRequest(HttpMethod.Get, "/health") {}.apply {
                            assertEquals(HttpStatusCode.OK, response.status())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `Gylding melding blir prosessert`() {
        val melding = gyldigMelding(
            fodselsnummerSoker = gyldigFodselsnummerA,
            fodselsnummerBarn = gyldigFodselsnummerB
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        kafkaTestConsumer.hentPreprosessertMelding(melding.soknadId)
    }

    @Test
    fun `Melding med språk og skal jobbe prosent blir prosessert`() {

        val sprak = "nn"
        val jobb1SkalJobbeProsent = 50.422
        val jobb2SkalJobberProsent = 12.111

        val melding = gyldigMelding(
            fodselsnummerSoker = gyldigFodselsnummerA,
            fodselsnummerBarn = gyldigFodselsnummerB,
            sprak = sprak,
            organisasjoner = listOf(
                Organisasjon("917755736", "Jobb1", skalJobbeProsent = jobb1SkalJobbeProsent),
                Organisasjon("917755737", "Jobb2", skalJobbeProsent = jobb2SkalJobberProsent)
            )
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        val oppgaveOpprettet = kafkaTestConsumer.hentPreprosessertMelding(melding.soknadId).data
        assertEquals(sprak, oppgaveOpprettet.sprak)
        assertEquals(2, oppgaveOpprettet.arbeidsgivere.organisasjoner.size)
        val jobb1 = oppgaveOpprettet.arbeidsgivere.organisasjoner.firstOrNull { it.navn == "Jobb1" }
        val jobb2 = oppgaveOpprettet.arbeidsgivere.organisasjoner.firstOrNull { it.navn == "Jobb2" }
        assertNotNull(jobb1)
        assertNotNull(jobb2)
        assertEquals(jobb1SkalJobbeProsent, jobb1.skalJobbeProsent)
        assertEquals(jobb2SkalJobberProsent, jobb2.skalJobbeProsent)
    }

    @Test
    fun `En feilprosessert melding vil bli prosessert etter at tjenesten restartes`() {
        val melding = gyldigMelding(
            fodselsnummerSoker = gyldigFodselsnummerA,
            fodselsnummerBarn = gyldigFodselsnummerB
        )

        wireMockServer.stubJournalfor(500) // Simulerer feil ved journalføring

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        ventPaaAtRetryMekanismeIStreamProsessering()
        readyGir200HealthGir503()

        wireMockServer.stubJournalfor(201) // Simulerer journalføring fungerer igjen
        restartEngine()
        kafkaTestConsumer.hentPreprosessertMelding(melding.soknadId)
    }

    private fun readyGir200HealthGir503() {
        with(engine) {
            handleRequest(HttpMethod.Get, "/isready") {}.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                handleRequest(HttpMethod.Get, "/health") {}.apply {
                    assertEquals(HttpStatusCode.ServiceUnavailable, response.status())
                }
            }
        }
    }

    @Test
    fun `Melding som gjeder søker med D-nummer`() {
        val melding = gyldigMelding(
            fodselsnummerSoker = dNummerA,
            fodselsnummerBarn = gyldigFodselsnummerB
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        kafkaTestConsumer.hentPreprosessertMelding(melding.soknadId)
    }

    @Test
    fun `Melding lagt til prosessering selv om sletting av vedlegg feiler`() {
        val melding = gyldigMelding(
            fodselsnummerSoker = gyldigFodselsnummerA,
            fodselsnummerBarn = gyldigFodselsnummerB,
            barnetsNavn = "kari",
            vedleggUrl = URI("http://localhost:8080/jeg-skal-feile/1")
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        kafkaTestConsumer.hentPreprosessertMelding(melding.soknadId)
    }

    @Test
    fun `Bruk barnets dnummer id til å slå opp i tps-proxy dersom navnet mangler`() {
        wireMockServer.stubAktoerRegister(dNummerA, "56789")
        wireMockServer.stubTpsProxyGetNavn("KLØKTIG", "BLUNKENDE", "SUPERKONSOLL")

        val melding = gyldigMelding(
            fodselsnummerSoker = gyldigFodselsnummerA,
            fodselsnummerBarn = dNummerA,
            barnetsNavn = null
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        val hentOpprettetOppgave: TopicEntry<PreprossesertMeldingV1> =
            kafkaTestConsumer.hentPreprosessertMelding(melding.soknadId)
        assertEquals("KLØKTIG BLUNKENDE SUPERKONSOLL", hentOpprettetOppgave.data.barn.navn)
    }

    @Test
    fun `Sjekker at dersom barnet ikke har mellomnavn så blir det ikke med i barnetsNavn`() {
        wireMockServer.stubAktoerRegister(dNummerA, "56789")
        wireMockServer.stubTpsProxyGetNavn("KLØKTIG", null, "SUPERKONSOLL")

        val melding = gyldigMelding(
            fodselsnummerSoker = gyldigFodselsnummerA,
            fodselsnummerBarn = dNummerA,
            barnetsNavn = null
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        val hentOpprettetOppgave: TopicEntry<PreprossesertMeldingV1> =
            kafkaTestConsumer.hentPreprosessertMelding(melding.soknadId)
        assertEquals("KLØKTIG SUPERKONSOLL", hentOpprettetOppgave.data.barn.navn)
    }


    @Test
    fun `Melding lagt til prosessering selv om oppslag paa aktoer ID for barn feiler`() {
        val melding = gyldigMelding(
            fodselsnummerSoker = gyldigFodselsnummerA,
            fodselsnummerBarn = gyldigFodselsnummerC
        )

        wireMockServer.stubAktoerRegisterGetAktoerIdNotFound(gyldigFodselsnummerC)

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        kafkaTestConsumer.hentPreprosessertMelding(melding.soknadId)
    }

    @Test
    fun `Bruk barnets fødselsnummer til å slå opp i tps-proxy dersom navnet mangler`() {
        wireMockServer.stubTpsProxyGetNavn("KLØKTIG", "BLUNKENDE", "SUPERKONSOLL")
        val melding = gyldigMelding(
            fodselsnummerSoker = gyldigFodselsnummerC,
            fodselsnummerBarn = gyldigFodselsnummerB,
            barnetsNavn = null
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        val hentOpprettetOppgave: TopicEntry<PreprossesertMeldingV1> =
            kafkaTestConsumer.hentPreprosessertMelding(melding.soknadId)
        assertEquals("KLØKTIG BLUNKENDE SUPERKONSOLL", hentOpprettetOppgave.data.barn.navn)
    }

    @Test
    fun `Bruk barnets aktørId til å slå opp i tps-proxy dersom navnet mangler`() {
        wireMockServer.stubAktoerRegister(dNummerA, "56789")
        wireMockServer.stubTpsProxyGetNavn("KLØKTIG", "BLUNKENDE", "SUPERKONSOLL")

        val melding = gyldigMelding(
            fodselsnummerSoker = gyldigFodselsnummerA,
            fodselsnummerBarn = null,
            barnetsNavn = null,
            aktoerIdBarn = "56789"
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        val hentOpprettetOppgave: TopicEntry<PreprossesertMeldingV1> =
            kafkaTestConsumer.hentPreprosessertMelding(melding.soknadId)
        assertEquals("KLØKTIG BLUNKENDE SUPERKONSOLL", hentOpprettetOppgave.data.barn.navn)
    }

    @Test
    fun `Forvent barnets fodselsnummer dersom den er satt i melding`() {
        wireMockServer.stubAktoerRegister(gyldigFodselsnummerB, "56789")

        val forventetFodselsNummer = gyldigFodselsnummerB

        val melding = gyldigMelding(
            fodselsnummerSoker = gyldigFodselsnummerA,
            fodselsnummerBarn = forventetFodselsNummer,
            barnetsNavn = null,
            aktoerIdBarn = "56789"
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        val hentOpprettetOppgave: TopicEntry<PreprossesertMeldingV1> =
            kafkaTestConsumer.hentPreprosessertMelding(melding.soknadId)
        assertEquals(forventetFodselsNummer, hentOpprettetOppgave.data.barn.fodselsnummer)
    }

    @Test
    fun `Forvent barnets fodselsnummer blir slått opp dersom den ikke er satt i melding`() {
        wireMockServer.stubAktoerRegister(gyldigFodselsnummerB, "666")
        val forventetFodselsNummer = gyldigFodselsnummerB

        val melding = gyldigMelding(
            fodselsnummerSoker = gyldigFodselsnummerA,
            fodselsnummerBarn = null,
            barnetsNavn = null,
            aktoerIdBarn = "666"
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        val hentOpprettetOppgave: TopicEntry<PreprossesertMeldingV1> =
            kafkaTestConsumer.hentPreprosessertMelding(melding.soknadId)
        assertEquals(forventetFodselsNummer, hentOpprettetOppgave.data.barn.fodselsnummer)
    }

    @Test
    fun `Forvent barnets fødselsdato`() {
        wireMockServer.stubAktoerRegister(gyldigFodselsnummerB, "666")

        val melding = gyldigMelding(
            fodselsnummerSoker = gyldigFodselsnummerA,
            fodselsnummerBarn = null,
            fodselsdatoBarn = LocalDate.now(),
            barnetsNavn = null,
            aktoerIdBarn = "666"
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        val hentOpprettetOppgave: TopicEntry<PreprossesertMeldingV1> =
            kafkaTestConsumer.hentPreprosessertMelding(melding.soknadId)
        assertEquals(LocalDate.now(), hentOpprettetOppgave.data.barn.fodselsdato)
    }

    @Test
    fun `Forvent korrekt format på melding sendt på journalført topic`() {
        wireMockServer.stubAktoerRegister(gyldigFodselsnummerB, "666")

        val melding = MeldingV1(
            sprak = "nb",
            soknadId = "583a3cf8-767a-49f4-a5dd-619df2c72c7a",
            mottatt = ZonedDateTime.parse("2019-10-20T07:15:36.124Z"),
            fraOgMed = LocalDate.parse("2020-01-06"),
            tilOgMed = LocalDate.parse("2020-01-10"),
            soker = Soker(
                aktoerId = "123456",
                fodselsnummer = gyldigFodselsnummerA,
                fornavn = "Ola",
                mellomnavn = "Mellomnavn",
                etternavn = "Nordmann"
            ),
            relasjonTilBarnet = "far",
            harMedsoker = false,
            barn = Barn(
                navn = "Bjarne",
                fodselsnummer = gyldigFodselsnummerB,
                fodselsdato = null,
                aktoerId = "666"
            ),
            arbeidsgivere = Arbeidsgivere(
                organisasjoner = listOf(
                    Organisasjon("917755736", "Jobb1", skalJobbeProsent = 50.25),
                    Organisasjon("917755737", "Jobb2", skalJobbeProsent = 20.0)
                )
            ),
            frilans = Frilans(
                harHattOppdragForFamilie = true,
                harHattInntektSomFosterforelder = true,
                startdato = LocalDate.now().minusYears(3),
                jobberFortsattSomFrilans = true,
                oppdrag = listOf(
                    Oppdrag(
                        arbeidsgivernavn = "Montesorri barnehage",
                        fraOgMed = LocalDate.parse("2019-10-01"),
                        tilOgMed = LocalDate.parse("2019-12-01"),
                        erPagaende = false
                    ),
                    Oppdrag(
                        arbeidsgivernavn = "Bislett Kebab House",
                        fraOgMed = LocalDate.parse("2019-12-02"),
                        tilOgMed = LocalDate.parse("2019-12-31"),
                        erPagaende = true
                    )
                )
            ),
            medlemskap = Medlemskap(
                harBoddIUtlandetSiste12Mnd = true,
                utenlandsoppholdSiste12Mnd = listOf(
                    Bosted(
                        fraOgMed = LocalDate.parse("2019-06-15"),
                        tilOgMed = LocalDate.parse("2019-06-28"),
                        landkode = "POL",
                        landnavn = "Polen"
                    ),
                    Bosted(
                        fraOgMed = LocalDate.parse("2019-07-01"),
                        tilOgMed = LocalDate.parse("2019-07-10"),
                        landkode = "DK",
                        landnavn = "Danmark"
                    )
                ),
                skalBoIUtlandetNeste12Mnd = true,
                utenlandsoppholdNeste12Mnd = listOf(
                    Bosted(
                        fraOgMed = LocalDate.parse("2020-06-15"),
                        tilOgMed = LocalDate.parse("2020-06-28"),
                        landkode = "AW",
                        landnavn = "Aruba"
                    ),
                    Bosted(
                        fraOgMed = LocalDate.parse("2020-07-01"),
                        tilOgMed = LocalDate.parse("2020-07-10"),
                        landkode = "BG",
                        landnavn = "Bulgaria"
                    )
                )
            ),
            utenlandsoppholdIPerioden = UtenlandsoppholdIPerioden(
                skalOppholdeSegIUtlandetIPerioden = true,
                opphold = listOf(
                    Utenlandsopphold(
                        fraOgMed = LocalDate.parse("2020-06-15"),
                        tilOgMed = LocalDate.parse("2020-06-28"),
                        landkode = "AW",
                        landnavn = "Aruba",
                        arsak = Arsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING, //barnetInnlagtIHelseinstitusjonForNorskOffentligRegning
                        erBarnetInnlagt = true,
                        erUtenforEos = false
                    )
                )
            ),
            beredskap = Beredskap(
                beredskap = true,
                tilleggsinformasjon = "I Beredskap"
            ),
            nattevaak = Nattevaak(
                harNattevaak = true,
                tilleggsinformasjon = "Har Nattevåk"
            ),
            tilsynsordning = Tilsynsordning(
                svar = "ja",
                ja = TilsynsordningJa(
                    mandag = Duration.parse("PT7H30M"),
                    tirsdag = Duration.parse("PT7H30M"),
                    onsdag = Duration.parse("PT7H30M"),
                    torsdag = Duration.parse("PT7H30M"),
                    fredag = Duration.parse("PT7H30M"),
                    tilleggsinformasjon = "Annet."
                ),
                vetIkke = null
            ),
            ferieuttakIPerioden = FerieuttakIPerioden(
                skalTaUtFerieIPerioden = true,
                ferieuttak = listOf(
                    Ferieuttak(LocalDate.parse("2020-01-07"), LocalDate.parse("2020-01-08")),
                    Ferieuttak(LocalDate.parse("2020-01-09"), LocalDate.parse("2020-01-10"))
                )
            ),
            grad = null,
            harBekreftetOpplysninger = true,
            harForstattRettigheterOgPlikter = true
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        val forventet: String = """
{
  "versjon": "1.0.0",
  "søknadId": "583a3cf8-767a-49f4-a5dd-619df2c72c7a",
  "mottattDato": "2019-10-20T07:15:36.124Z",
  "språk": "nb",
  "søker": {
    "norskIdentitetsnummer": "02119970078"
  },
  "perioder": {
    "2020-01-06/2020-01-10": {}
  },
  "barn": {
    "fødselsdato": null,
    "norskIdentitetsnummer": "19066672169"
  },
  "bosteder": {
    "perioder": {
      "2019-06-15/2019-06-28": {
        "land": "POL"
      },
      "2019-07-01/2019-07-10": {
        "land": "DK"
      }
    }
  },
  "utenlandsopphold": {
    "perioder": {
      "2020-06-15/2020-06-28": {
        "land": "AW",
        "årsak": "barnetInnlagtIHelseinstitusjonForNorskOffentligRegning"
      }
    }
  },
  "beredskap": {
    "perioder": {
      "2020-01-06/2020-01-10": {
        "tilleggsinformasjon": "I Beredskap"
      }
    }
  },
  "nattevåk": {
    "perioder": {
      "2020-01-06/2020-01-10": {
        "tilleggsinformasjon": "Har Nattevåk"
      }
    }
  },
  "tilsynsordning": {
    "iTilsynsordning": "ja",
    "opphold": {
      "2020-01-06/2020-01-10": {
        "lengde": "PT37H30M"
      }
    }
  },
  "arbeid": {
    "arbeidstaker": [
      {
        "organisasjonsnummer": "917755736",
        "norskIdentitetsnummer": null,
        "perioder": {
          "2020-01-06/2020-01-10": {
            "skalJobbeProsent": 50.25
          }
        }
      },
      {
        "organisasjonsnummer": "917755737",
        "norskIdentitetsnummer": null,
        "perioder": {
          "2020-01-06/2020-01-10": {
            "skalJobbeProsent": 20.00
          }
        }
      }
    ],
    "frilanser": [
      {
        "perioder": {
          "2019-10-01/2019-12-01": {},
          "2019-12-02/2019-12-31": {}
        }
      }
    ]
  },
  "lovbestemtFerie": {
    "perioder": {
      "2020-01-07/2020-01-08": {},
      "2020-01-09/2020-01-10": {}
    }
  }
}
        """.trimIndent()
        val journalførtMelding: TopicEntry<Journalfort> = journalførtConsumer.hentJournalførtMelding(melding.soknadId)
        val joournalførtMeldingJson = journalførtMelding.data.søknad.toString()
        assertNotNull(journalførtMelding)
        JSONAssert.assertEquals(forventet, joournalførtMeldingJson, false)
    }


    private fun gyldigMelding(
        fodselsnummerSoker: String,
        fodselsnummerBarn: String?,
        vedleggUrl: URI = URI("${wireMockServer.getK9DokumentBaseUrl()}/v1/dokument/${UUID.randomUUID()}"),
        barnetsNavn: String? = "kari",
        fodselsdatoBarn: LocalDate? = LocalDate.now(),
        aktoerIdBarn: String? = null,
        sprak: String? = null,
        organisasjoner: List<Organisasjon> = listOf(
            Organisasjon("917755736", "Gyldig")
        )
    ): MeldingV1 = MeldingV1(
        sprak = sprak,
        soknadId = UUID.randomUUID().toString(),
        mottatt = ZonedDateTime.now(),
        fraOgMed = LocalDate.now(),
        tilOgMed = LocalDate.now().plusWeeks(1),
        soker = Soker(
            aktoerId = "123456",
            fodselsnummer = fodselsnummerSoker,
            etternavn = "Nordmann",
            mellomnavn = "Mellomnavn",
            fornavn = "Ola"
        ),
        barn = Barn(
            navn = barnetsNavn,
            fodselsnummer = fodselsnummerBarn,
            fodselsdato = fodselsdatoBarn,
            aktoerId = aktoerIdBarn
        ),
        relasjonTilBarnet = "Mor",
        arbeidsgivere = Arbeidsgivere(
            organisasjoner = organisasjoner
        ),
        vedleggUrls = listOf(vedleggUrl),
        medlemskap = Medlemskap(
            harBoddIUtlandetSiste12Mnd = true,
            skalBoIUtlandetNeste12Mnd = true
        ),
        harMedsoker = true,
        grad = 70,
        harBekreftetOpplysninger = true,
        harForstattRettigheterOgPlikter = true,
        tilsynsordning = Tilsynsordning(
            svar = "ja",
            ja = TilsynsordningJa(
                mandag = Duration.ofHours(5),
                tirsdag = Duration.ofHours(4),
                onsdag = Duration.ofHours(3).plusMinutes(45),
                torsdag = Duration.ofHours(2),
                fredag = Duration.ofHours(1).plusMinutes(30),
                tilleggsinformasjon = "Litt tilleggsinformasjon."
            ),
            vetIkke = null
        ),
        beredskap = Beredskap(
            beredskap = true,
            tilleggsinformasjon = "I Beredskap"
        ),
        nattevaak = Nattevaak(
            harNattevaak = true,
            tilleggsinformasjon = "Har Nattevåk"
        ),
        utenlandsoppholdIPerioden = UtenlandsoppholdIPerioden(
            skalOppholdeSegIUtlandetIPerioden = false,
            opphold = listOf()
        ),
        ferieuttakIPerioden = FerieuttakIPerioden(skalTaUtFerieIPerioden = false, ferieuttak = listOf()),
        frilans = Frilans(
            harHattOppdragForFamilie = true,
            harHattInntektSomFosterforelder = true,
            startdato = LocalDate.now().minusYears(3),
            jobberFortsattSomFrilans = true,
            oppdrag = listOf(
                Oppdrag(
                    arbeidsgivernavn = "Montesorri barnehage",
                    fraOgMed = LocalDate.now().minusYears(2),
                    tilOgMed = null,
                    erPagaende = true
                )
            )
        )
    )

    private fun ventPaaAtRetryMekanismeIStreamProsessering() = runBlocking { delay(Duration.ofSeconds(30)) }
}