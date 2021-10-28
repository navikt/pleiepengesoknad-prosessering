package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import no.nav.common.KafkaEnvironment
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.felles.*
import no.nav.helse.k9format.assertJournalførtFormat
import no.nav.helse.kafka.TopicEntry
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.PreprossesertMeldingV1
import org.junit.AfterClass
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


class PleiepengesoknadProsesseringTest {

    private companion object {

        private val logger: Logger = LoggerFactory.getLogger(PleiepengesoknadProsesseringTest::class.java)

        private val wireMockServer: WireMockServer = WireMockBuilder()
            .withNaisStsSupport()
            .withAzureSupport()
            .build()
            .stubK9DokumentHealth()
            .stubPleiepengerJoarkHealth()
            .stubJournalfor()
            .stubLagreDokument()
            .stubSlettDokument()

        private val kafkaEnvironment = KafkaWrapper.bootstrap()
        private val kafkaTestConsumer = kafkaEnvironment.testConsumer()
        private val cleanupConsumer = kafkaEnvironment.cleanupConsumer()
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

        @AfterClass
        @JvmStatic
        fun tearDown() {
            logger.info("Tearing down")
            wireMockServer.stop()
            kafkaTestConsumer.close()
            kafkaTestProducer.close()
            cleanupConsumer.close()
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
        cleanupConsumer
            .hentCleanupMelding(melding.søknadId)
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
        cleanupConsumer
            .hentCleanupMelding(melding.søknadId)
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
        cleanupConsumer
            .hentCleanupMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `Melding lagt til prosessering selv om sletting av vedlegg feiler`() {
        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            vedleggUrls = listOf(URI("http://localhost:8080/jeg-skal-feile/1"))
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        cleanupConsumer
            .hentCleanupMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `Bruk barnets dnummer id`() {

        val defaultBarn = SøknadUtils.defaultSøknad.barn
        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            barn = defaultBarn.copy(fødselsnummer = dNummerA, navn = "Barn med D-nummer")
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        val preprosessertMelding: TopicEntry<PreprossesertMeldingV1> =
            kafkaTestConsumer.hentPreprosessertMelding(melding.søknadId)
        assertEquals("Barn med D-nummer", preprosessertMelding.data.barn.navn)
        cleanupConsumer
            .hentCleanupMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `Forvent barnets fodselsnummer dersom den er satt i melding`() {

        val forventetFodselsNummer = gyldigFodselsnummerB

        val defaultBarn = SøknadUtils.defaultSøknad.barn
        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            barn = defaultBarn.copy(fødselsnummer = forventetFodselsNummer, navn = "KLØKTIG SUPERKONSOLL")
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        val preprosessertMelding: TopicEntry<PreprossesertMeldingV1> =
            kafkaTestConsumer.hentPreprosessertMelding(melding.søknadId)
        assertEquals(forventetFodselsNummer, preprosessertMelding.data.barn.fødselsnummer)
        cleanupConsumer
            .hentCleanupMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `Forvent korrekt format på melding sendt på journalført topic`() {

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
            harMedsøker = false,
            barn = Barn(
                navn = "Bjarne",
                fødselsnummer = gyldigFodselsnummerB
            ),
            frilans = Frilans(
                startdato = LocalDate.parse("2018-08-01"),
                jobberFortsattSomFrilans = true
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
            ferieuttakIPerioden = FerieuttakIPerioden(
                skalTaUtFerieIPerioden = true,
                ferieuttak = listOf(
                    Ferieuttak(LocalDate.parse("2020-01-07"), LocalDate.parse("2020-01-08")),
                    Ferieuttak(LocalDate.parse("2020-01-09"), LocalDate.parse("2020-01-10"))
                )
            ),
            samtidigHjemme = true,
            harVærtEllerErVernepliktig = false,
            harBekreftetOpplysninger = true,
            harForståttRettigheterOgPlikter = true,
            k9FormatSøknad = SøknadUtils.defaultK9FormatPSB()
        )

        kafkaTestProducer.leggSoknadTilProsessering(melding)

        cleanupConsumer
            .hentCleanupMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    private fun ventPaaAtRetryMekanismeIStreamProsessering() = runBlocking { delay(Duration.ofSeconds(30)) }
}
