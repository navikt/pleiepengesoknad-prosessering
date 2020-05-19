package no.nav.helse

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
import no.nav.helse.dusseldorf.ktor.testsupport.wiremock.WireMockBuilder
import no.nav.helse.k9format.assertJournalførtFormat
import no.nav.helse.prosessering.v1.*
import no.nav.helse.prosessering.v1.asynkron.TopicEntry
import org.junit.AfterClass
import org.junit.BeforeClass
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

        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString()
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        journalførtConsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `Melding med språk og skal jobbe prosent blir prosessert`() {

        val språk = "nn"
        val jobb1SkalJobbeProsent = 50.422
        val jobb2SkalJobberProsent = 12.111

        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            språk = språk,
            arbeidsgivere = Arbeidsgivere(
                organisasjoner = listOf(
                    Organisasjon(
                        "917755736",
                        "Jobb1",
                        skalJobbeProsent = jobb1SkalJobbeProsent,
                        jobberNormaltTimer = 37.5,
                        skalJobbe = "redusert"
                    ),
                    Organisasjon(
                        "917755737",
                        "Jobb2",
                        skalJobbeProsent = jobb2SkalJobberProsent,
                        jobberNormaltTimer = 37.5,
                        skalJobbe = "redusert"
                    )
                )
            )
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        val preprossesertMelding = kafkaTestConsumer.hentPreprosessertMelding(melding.søknadId).data
        assertEquals(språk, preprossesertMelding.språk)
        assertEquals(2, preprossesertMelding.arbeidsgivere.organisasjoner.size)
        val jobb1 = preprossesertMelding.arbeidsgivere.organisasjoner.firstOrNull { it.navn == "Jobb1" }
        val jobb2 = preprossesertMelding.arbeidsgivere.organisasjoner.firstOrNull { it.navn == "Jobb2" }
        assertNotNull(jobb1)
        assertNotNull(jobb2)
        assertEquals(jobb1SkalJobbeProsent, jobb1.skalJobbeProsent)
        assertEquals(jobb2SkalJobberProsent, jobb2.skalJobbeProsent)

        journalførtConsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `En feilprosessert melding vil bli prosessert etter at tjenesten restartes`() {
        val melding = SøknadUtils.defaultSøknad

        wireMockServer.stubJournalfor(500) // Simulerer feil ved journalføring

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        ventPaaAtRetryMekanismeIStreamProsessering()
        readyGir200HealthGir503()

        wireMockServer.stubJournalfor(201) // Simulerer journalføring fungerer igjen
        restartEngine()
        journalførtConsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
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
        val melding = SøknadUtils.defaultSøknad

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        journalførtConsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `Melding lagt til prosessering selv om sletting av vedlegg feiler`() {
        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            vedleggUrls = listOf(URI("http://localhost:8080/jeg-skal-feile/1"))
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        journalførtConsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `Bruk barnets dnummer id til å slå opp i tps-proxy dersom navnet mangler`() {
        wireMockServer.stubAktoerRegister(dNummerA, "56789")
        wireMockServer.stubTpsProxyGetNavn("KLØKTIG", "BLUNKENDE", "SUPERKONSOLL")

        val defaultBarn = SøknadUtils.defaultSøknad.barn
        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            barn = defaultBarn.copy(fødselsnummer = dNummerA, navn = null)
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        val preprosessertMelding: TopicEntry<PreprossesertMeldingV1> =
            kafkaTestConsumer.hentPreprosessertMelding(melding.søknadId)
        assertEquals("KLØKTIG BLUNKENDE SUPERKONSOLL", preprosessertMelding.data.barn.navn)
        journalførtConsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `Sjekker at dersom barnet ikke har mellomnavn så blir det ikke med i barnetsNavn`() {
        wireMockServer.stubAktoerRegister(dNummerA, "56789")
        wireMockServer.stubTpsProxyGetNavn("KLØKTIG", null, "SUPERKONSOLL")

        val defaultBarn = SøknadUtils.defaultSøknad.barn
        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            barn = defaultBarn.copy(fødselsnummer = dNummerA, navn = null)
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        val preprosessertMelding: TopicEntry<PreprossesertMeldingV1> =
            kafkaTestConsumer.hentPreprosessertMelding(melding.søknadId)
        assertEquals("KLØKTIG SUPERKONSOLL", preprosessertMelding.data.barn.navn)
        journalførtConsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `Melding lagt til prosessering selv om oppslag paa aktoer ID for barn feiler`() {
        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString()
        )

        wireMockServer.stubAktoerRegisterGetAktoerIdNotFound(gyldigFodselsnummerC)

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        journalførtConsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `Bruk barnets fødselsnummer til å slå opp i tps-proxy dersom navnet mangler`() {
        wireMockServer.stubTpsProxyGetNavn("KLØKTIG", "BLUNKENDE", "SUPERKONSOLL")
        val defaultBarn = SøknadUtils.defaultSøknad.barn
        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            barn = defaultBarn.copy(navn = null)
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        val preprosessertMelding: TopicEntry<PreprossesertMeldingV1> =
            kafkaTestConsumer.hentPreprosessertMelding(melding.søknadId)
        assertEquals("KLØKTIG BLUNKENDE SUPERKONSOLL", preprosessertMelding.data.barn.navn)
        journalførtConsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `Bruk barnets aktørId til å slå opp i tps-proxy dersom navnet mangler`() {
        wireMockServer.stubAktoerRegister(dNummerA, "56789")
        wireMockServer.stubTpsProxyGetNavn("KLØKTIG", "BLUNKENDE", "SUPERKONSOLL")

        val defaultBarn = SøknadUtils.defaultSøknad.barn
        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            barn = defaultBarn.copy(fødselsnummer = null, navn = null, aktørId = "56789")
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        val preprosessertMelding: TopicEntry<PreprossesertMeldingV1> =
            kafkaTestConsumer.hentPreprosessertMelding(melding.søknadId)
        assertEquals("KLØKTIG BLUNKENDE SUPERKONSOLL", preprosessertMelding.data.barn.navn)
        journalførtConsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `Forvent barnets fodselsnummer dersom den er satt i melding`() {
        wireMockServer.stubAktoerRegister(gyldigFodselsnummerB, "56789")

        val forventetFodselsNummer = gyldigFodselsnummerB

        val defaultBarn = SøknadUtils.defaultSøknad.barn
        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            barn = defaultBarn.copy(fødselsnummer = forventetFodselsNummer, navn = null, aktørId = "56789")
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        val preprosessertMelding: TopicEntry<PreprossesertMeldingV1> =
            kafkaTestConsumer.hentPreprosessertMelding(melding.søknadId)
        assertEquals(forventetFodselsNummer, preprosessertMelding.data.barn.fødselsnummer)
        journalførtConsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `Forvent barnets fodselsnummer blir slått opp dersom den ikke er satt i melding`() {
        wireMockServer.stubAktoerRegister(gyldigFodselsnummerB, "666")
        val forventetFodselsNummer = gyldigFodselsnummerB

        val defaultBarn = SøknadUtils.defaultSøknad.barn
        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            barn = defaultBarn.copy(fødselsnummer = forventetFodselsNummer, navn = null, aktørId = "666")
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        val preprosessertMelding: TopicEntry<PreprossesertMeldingV1> =
            kafkaTestConsumer.hentPreprosessertMelding(melding.søknadId)
        assertEquals(forventetFodselsNummer, preprosessertMelding.data.barn.fødselsnummer)
        journalførtConsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `Forvent barnets fødselsdato`() {
        wireMockServer.stubAktoerRegister(gyldigFodselsnummerB, "666")

        val defaultBarn = SøknadUtils.defaultSøknad.barn
        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            barn = defaultBarn.copy(fødselsdato = LocalDate.now(), navn = null, aktørId = "666")
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        val preprosesssertMelding: TopicEntry<PreprossesertMeldingV1> =
            kafkaTestConsumer.hentPreprosessertMelding(melding.søknadId)
        assertEquals(LocalDate.now(), preprosesssertMelding.data.barn.fødselsdato)
        journalførtConsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `Forvent korrekt format på melding sendt på journalført topic`() {
        wireMockServer.stubAktoerRegister(gyldigFodselsnummerB, "666")

        val melding = MeldingV1(
            språk = "nb",
            søknadId = "583a3cf8-767a-49f4-a5dd-619df2c72c7a",
            mottatt = ZonedDateTime.parse("2019-10-20T07:15:36.124Z"),
            fraOgMed = LocalDate.parse("2020-01-06"),
            tilOgMed = LocalDate.parse("2020-01-10"),
            søker = Søker(
                aktørId = "123456",
                fødselsnummer = gyldigFodselsnummerA,
                fornavn = "Ola",
                mellomnavn = "Mellomnavn",
                etternavn = "Nordmann"
            ),
            relasjonTilBarnet = "far",
            harMedsøker = false,
            barn = Barn(
                navn = "Bjarne",
                fødselsnummer = gyldigFodselsnummerB,
                fødselsdato = null,
                aktørId = "666"
            ),
            arbeidsgivere = Arbeidsgivere(
                organisasjoner = listOf(
                    Organisasjon(
                        "917755736",
                        "Jobb1",
                        skalJobbeProsent = 50.25,
                        jobberNormaltTimer = 5.0,
                        skalJobbe = "redusert"
                    ),
                    Organisasjon(
                        "917755737",
                        "Jobb2",
                        skalJobbeProsent = 20.0,
                        jobberNormaltTimer = 3.75,
                        skalJobbe = "redusert"
                    )
                )
            ),
            frilans = Frilans(
                startdato = LocalDate.parse("2018-08-01"),
                jobberFortsattSomFrilans = true
            ),
            selvstendigVirksomheter = listOf(
                Virksomhet(
                    næringstyper = listOf(),
                    fraOgMed = LocalDate.parse("2020-01-01"),
                    næringsinntekt = 123456,
                    navnPåVirksomheten = "Virksomehet 1",
                    registrertINorge = true,
                    organisasjonsnummer = "12345"
                ),
                Virksomhet(
                    næringstyper = listOf(),
                    fraOgMed = LocalDate.parse("2020-02-01"),
                    tilOgMed = LocalDate.parse("2020-05-01"),
                    næringsinntekt = 123456,
                    navnPåVirksomheten = "Virksomehet 2",
                    registrertINorge = false,
                    registrertIUtlandet = Land(
                        landkode = "DEU",
                        landnavn = "Tyskland"
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
                        årsak = Årsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING, //barnetInnlagtIHelseinstitusjonForNorskOffentligRegning
                        erBarnetInnlagt = true,
                        erUtenforEøs = false
                    )
                )
            ),
            beredskap = Beredskap(
                beredskap = true,
                tilleggsinformasjon = "I Beredskap"
            ),
            nattevåk = Nattevåk(
                harNattevåk = true,
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
            harBekreftetOpplysninger = true,
            harForståttRettigheterOgPlikter = true
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)

        journalførtConsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `tilsynsordning ja`() {
        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
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
            )
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)

        journalførtConsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `tilsynsordning nei`() {
        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            tilsynsordning = Tilsynsordning(
                svar = "nei",
                ja = null,
                vetIkke = null
            )
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)

        journalførtConsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `tilsynsordning vet_ikke`() {
        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            tilsynsordning = Tilsynsordning(
                svar = "vetIkke",
                ja = null,
                vetIkke = TilsynsordningVetIkke(
                    svar = "vetIkke",
                    annet = "annet"
                )
            )
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)

        journalførtConsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `tilsynsordning vet_ikke uten annet felt satt`() {
        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            tilsynsordning = Tilsynsordning(
                svar = "vetIkke",
                ja = null,
                vetIkke = TilsynsordningVetIkke(
                    svar = "hva som helst?"
                )
            )
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)

        journalførtConsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `tilsynsordning vet_ikke med annet felt satt`() {
        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            tilsynsordning = Tilsynsordning(
                svar = "vetIkke",
                ja = null,
                vetIkke = TilsynsordningVetIkke(
                    svar = "hva som helst?",
                    annet = "annet grunn"
                )
            )
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)

        journalførtConsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `Søknad uten tilsynsordning satt`() {
        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            tilsynsordning = null
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)

        journalførtConsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    private fun ventPaaAtRetryMekanismeIStreamProsessering() = runBlocking { delay(Duration.ofSeconds(30)) }
}
