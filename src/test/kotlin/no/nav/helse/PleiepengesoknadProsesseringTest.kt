package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.typesafe.config.ConfigFactory
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import no.nav.helse.EndringsmeldingUtils.defaultEndringsmelding
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.felles.Barn
import no.nav.helse.felles.Beredskap
import no.nav.helse.felles.Bosted
import no.nav.helse.felles.Ferieuttak
import no.nav.helse.felles.FerieuttakIPerioden
import no.nav.helse.felles.Frilans
import no.nav.helse.felles.Land
import no.nav.helse.felles.Medlemskap
import no.nav.helse.felles.Nattevåk
import no.nav.helse.felles.Næringstyper
import no.nav.helse.felles.OpptjeningIUtlandet
import no.nav.helse.felles.OpptjeningType
import no.nav.helse.felles.SelvstendigNæringsdrivende
import no.nav.helse.felles.Søker
import no.nav.helse.felles.UtenlandskNæring
import no.nav.helse.felles.Utenlandsopphold
import no.nav.helse.felles.UtenlandsoppholdIPerioden
import no.nav.helse.felles.Årsak
import no.nav.helse.k9format.assertJournalførtFormat
import no.nav.helse.kafka.TopicEntry
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.PreprossesertMeldingV1
import no.nav.helse.prosessering.v1.asynkron.Cleanup
import no.nav.helse.prosessering.v1.asynkron.CleanupEndringsmelding
import no.nav.helse.prosessering.v1.asynkron.EndringsmeldingTopics
import no.nav.helse.prosessering.v1.asynkron.SøknadTopics
import no.nav.helse.prosessering.v1.asynkron.endringsmelding.EndringsmeldingV1
import no.nav.helse.prosessering.v1.asynkron.endringsmelding.PreprossesertEndringsmeldingV1
import org.junit.AfterClass
import org.junit.jupiter.api.Assertions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.KafkaContainer
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class PleiepengesoknadProsesseringTest {

    private companion object {

        private val logger: Logger = LoggerFactory.getLogger(PleiepengesoknadProsesseringTest::class.java)

        private val wireMockServer: WireMockServer = WireMockBuilder()
            .withAzureSupport()
            .build()
            .stubK9MellomlagringHealth()
            .stubPleiepengerJoarkHealth()
            .stubJournalfor()
            .stubLagreDokument()
            .stubSlettDokument()

        private val kafkaEnvironment = KafkaWrapper.bootstrap()
        private val søknadcleanupConsumer = kafkaEnvironment.cleanupConsumer<Cleanup>(
            topic = SøknadTopics.CLEANUP,
            consumerClientId = "sif-innsyn-api"
        )

        private val søknadKafkaProducer = kafkaEnvironment.testProducer<MeldingV1>(
            producerClientId = "pleiepengesoknad-api",
            topic = SøknadTopics.MOTTATT_v2
        )

        private val søknadKafkaConsumer =
            kafkaEnvironment.testConsumer<PreprossesertMeldingV1>(topic = SøknadTopics.PREPROSSESERT)

        private val endringsmeldingKafkaProducer = kafkaEnvironment.testProducer<EndringsmeldingV1>(
            producerClientId = "pleiepengesoknad-api",
            topic = EndringsmeldingTopics.ENDRINGSMELDING_MOTTATT
        )

        private val endringsmeldingKafkConsumer =
            kafkaEnvironment.testConsumer<PreprossesertEndringsmeldingV1>(
                topic = EndringsmeldingTopics.ENDRINGSMELDING_PREPROSSESERT
            )

        private val endringsmeldingCleanupConsumer =
            kafkaEnvironment.cleanupConsumer<CleanupEndringsmelding>(
                topic = EndringsmeldingTopics.ENDRINGSMELDING_CLEANUP,
                consumerClientId = "sif-innsyn-api"
            )

        // Se https://github.com/navikt/dusseldorf-ktor#f%C3%B8dselsnummer
        private val gyldigFodselsnummerA = "02119970078"
        private val gyldigFodselsnummerB = "19066672169"
        private val gyldigFodselsnummerC = "20037473937"
        private val dNummerA = "55125314561"

        private var engine = newEngine(kafkaEnvironment).apply {
            start(wait = true)
        }

        private fun getConfig(kafkaContainer: KafkaContainer?): ApplicationConfig {
            val fileConfig = ConfigFactory.load()
            val testConfig = ConfigFactory.parseMap(
                TestConfiguration.asMap(
                    wireMockServer = wireMockServer,
                    kafkaContainer = kafkaContainer
                )
            )
            val mergedConfig = testConfig.withFallback(fileConfig)
            return HoconApplicationConfig(mergedConfig)
        }

        private fun newEngine(kafkaContainer: KafkaContainer) = TestApplicationEngine(createTestEnvironment {
            config = getConfig(kafkaContainer)
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

            søknadKafkaProducer.close()
            søknadKafkaConsumer.close()
            søknadcleanupConsumer.close()

            endringsmeldingKafkaProducer.close()
            endringsmeldingKafkConsumer.close()
            endringsmeldingCleanupConsumer.close()

            stopEngine()
            kafkaEnvironment.stop()

            logger.info("Tear down complete")
        }
    }

    @org.junit.jupiter.api.Test
    fun `test isready, isalive, health og metrics`() {
        with(engine) {
            val healthEndpoints = listOf("/isready", "/isalive", "/metrics", "/health")

            val responses = healthEndpoints.map { endpoint ->
                handleRequest(HttpMethod.Get, endpoint).response.status()
            }

            for(statusCode in responses) {
                Assertions.assertEquals(HttpStatusCode.OK, statusCode)
            }

        }
    }

    @Test
    fun `Gylding melding blir prosessert`() {

        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString()
        )

        søknadKafkaProducer.leggPåMelding(melding.søknadId, melding, topic = SøknadTopics.MOTTATT_v2)
        søknadcleanupConsumer
            .hentCleanupMelding(melding.søknadId, topic = SøknadTopics.CLEANUP)
            .assertJournalførtFormat()
    }

    @Test
    fun `Melding som gjeder søker med D-nummer`() {
        val melding = SøknadUtils.defaultSøknad

        søknadKafkaProducer.leggPåMelding(melding.søknadId, melding, topic = SøknadTopics.MOTTATT_v2)
        søknadcleanupConsumer
            .hentCleanupMelding(melding.søknadId, topic = SøknadTopics.CLEANUP)
            .assertJournalførtFormat()
    }

    @Test
    fun `Melding lagt til prosessering selv om sletting av vedlegg feiler`() {
        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            vedleggId = listOf("1")
        )

        søknadKafkaProducer.leggPåMelding(melding.søknadId, melding, topic = SøknadTopics.MOTTATT_v2)
        søknadcleanupConsumer
            .hentCleanupMelding(melding.søknadId, topic = SøknadTopics.CLEANUP)
            .assertJournalførtFormat()
    }

    @Test
    fun `Bruk barnets dnummer id`() {

        val defaultBarn = SøknadUtils.defaultSøknad.barn
        val melding = SøknadUtils.defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            barn = defaultBarn.copy(fødselsnummer = dNummerA, navn = "Barn med D-nummer")
        )

        søknadKafkaProducer.leggPåMelding(melding.søknadId, melding, topic = SøknadTopics.MOTTATT_v2)

        val preprosessertMelding: TopicEntry<PreprossesertMeldingV1> =
            søknadKafkaConsumer.hentMelding(melding.søknadId, topic = SøknadTopics.PREPROSSESERT)

        assertEquals("Barn med D-nummer", preprosessertMelding.data.barn.navn)
        søknadcleanupConsumer
            .hentCleanupMelding(melding.søknadId, topic = SøknadTopics.CLEANUP)
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

        søknadKafkaProducer.leggPåMelding(melding.søknadId, melding, topic = SøknadTopics.MOTTATT_v2)
        val preprosessertMelding: TopicEntry<PreprossesertMeldingV1> =
            søknadKafkaConsumer.hentMelding(melding.søknadId, topic = SøknadTopics.PREPROSSESERT)
        assertEquals(forventetFodselsNummer, preprosessertMelding.data.barn.fødselsnummer)
        søknadcleanupConsumer
            .hentCleanupMelding(melding.søknadId, topic = SøknadTopics.CLEANUP)
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
                fødselsnummer = gyldigFodselsnummerB,
                aktørId = "11111111111"
            ),
            frilans = Frilans(
                startdato = LocalDate.parse("2018-08-01"),
                jobberFortsattSomFrilans = true,
                harInntektSomFrilanser = true
            ),
            arbeidsgivere = listOf(),
            selvstendigNæringsdrivende = SelvstendigNæringsdrivende(harInntektSomSelvstendig = false),
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
            utenlandskNæring = listOf(
                UtenlandskNæring(
                    næringstype = Næringstyper.DAGMAMMA,
                    navnPåVirksomheten = "Dagmamma AS",
                    land = Land(landkode = "NDL", landnavn = "Nederland"),
                    fraOgMed = LocalDate.parse("2020-01-01")
                )
            ),
            opptjeningIUtlandet = listOf(
                OpptjeningIUtlandet(
                    navn = "Yolo AS",
                    opptjeningType = OpptjeningType.ARBEIDSTAKER,
                    land = Land(landkode = "NDL", landnavn = "Nederland"),
                    fraOgMed = LocalDate.parse("2020-01-01"),
                    tilOgMed = LocalDate.parse("2020-10-01")
                )
            ),
            harVærtEllerErVernepliktig = false,
            harBekreftetOpplysninger = true,
            harForståttRettigheterOgPlikter = true,
            k9FormatSøknad = SøknadUtils.defaultK9FormatPSB(),
            vedleggId = listOf(),
            omsorgstilbud = null,
            barnRelasjon = null,
            barnRelasjonBeskrivelse = null
        )

        søknadKafkaProducer.leggPåMelding(melding.søknadId, melding, topic = SøknadTopics.MOTTATT_v2)

        søknadcleanupConsumer
            .hentCleanupMelding(melding.søknadId, topic = SøknadTopics.CLEANUP)
            .assertJournalførtFormat()
    }

    @Test
    fun `En feilprosessert melding vil bli prosessert etter at tjenesten restartes`() {
        val melding = SøknadUtils.defaultSøknad.copy(søknadId = UUID.randomUUID().toString())

        wireMockServer.stubJournalfor(500) // Simulerer feil ved journalføring

        søknadKafkaProducer.leggPåMelding(melding.søknadId, melding, topic = SøknadTopics.MOTTATT_v2)
        ventPaaAtRetryMekanismeIStreamProsessering()
        readyGir200HealthGir503()

        wireMockServer.stubJournalfor(201) // Simulerer journalføring fungerer igjen
        restartEngine()
        søknadcleanupConsumer
            .hentCleanupMelding(melding.søknadId, topic = SøknadTopics.CLEANUP, maxWaitInSeconds = 120)
            .assertJournalførtFormat()
    }

    @Test
    fun endringsmelding() {
        val søknadsId = UUID.randomUUID()
        val endringsmelding = defaultEndringsmelding(søknadsId)
        endringsmeldingKafkaProducer.leggPåMelding(
            søknadsId.toString(),
            endringsmelding,
            EndringsmeldingTopics.ENDRINGSMELDING_MOTTATT
        )

        val preprosessertEndringsMelding: TopicEntry<PreprossesertEndringsmeldingV1> =
            endringsmeldingKafkConsumer.hentMelding(
                soknadId = søknadsId.toString(),
                topic = EndringsmeldingTopics.ENDRINGSMELDING_PREPROSSESERT
            )

        assertNotNull(preprosessertEndringsMelding)
        val cleanupEndringsmelding = endringsmeldingCleanupConsumer.hentCleanupMelding(
            soknadId = søknadsId.toString(),
            topic = EndringsmeldingTopics.ENDRINGSMELDING_CLEANUP
        )
        assertNotNull(cleanupEndringsmelding)
        cleanupEndringsmelding.assertJournalførtFormat()
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

    private fun ventPaaAtRetryMekanismeIStreamProsessering() = runBlocking { delay(Duration.ofSeconds(30)) }
}
