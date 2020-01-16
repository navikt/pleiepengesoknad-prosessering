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
import no.nav.helse.prosessering.v1.*
import no.nav.helse.prosessering.v1.asynkron.OppgaveOpprettet
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
            .stubPleiepengerOppgaveHealth()
            .stubJournalfor()
            .stubOpprettOppgave()
            .stubLagreDokument()
            .stubSlettDokument()
            .stubAktoerRegister("29099012345", "123456")

        private val kafkaEnvironment = KafkaWrapper.bootstrap()
        private val kafkaTestConsumer = kafkaEnvironment.testConsumer()
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
        kafkaTestConsumer.hentOpprettetOppgave(melding.soknadId)
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
        val oppgaveOpprettet = kafkaTestConsumer.hentOpprettetOppgave(melding.soknadId).data
        assertEquals(sprak, oppgaveOpprettet.melding.sprak)
        assertEquals(2, oppgaveOpprettet.melding.arbeidsgivere.organisasjoner.size)
        val jobb1 = oppgaveOpprettet.melding.arbeidsgivere.organisasjoner.firstOrNull { it.navn == "Jobb1" }
        val jobb2 = oppgaveOpprettet.melding.arbeidsgivere.organisasjoner.firstOrNull { it.navn == "Jobb2" }
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
        kafkaTestConsumer.hentOpprettetOppgave(melding.soknadId)
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
        kafkaTestConsumer.hentOpprettetOppgave(melding.soknadId)
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
        kafkaTestConsumer.hentOpprettetOppgave(melding.soknadId)
    }

    @Test
    fun `Melding lagt til prosessering selv om oppslag paa aktoer ID for barn feiler`() {
        val melding = gyldigMelding(
            fodselsnummerSoker = gyldigFodselsnummerA,
            fodselsnummerBarn = gyldigFodselsnummerC
        )

        wireMockServer.stubAktoerRegisterGetAktoerIdNotFound(gyldigFodselsnummerC)

        kafkaTestProducer.leggSoknadTilProsessering(melding)
        kafkaTestConsumer.hentOpprettetOppgave(melding.soknadId)
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
        val hentOpprettetOppgave: TopicEntry<OppgaveOpprettet> = kafkaTestConsumer.hentOpprettetOppgave(melding.soknadId)
        assertEquals("KLØKTIG BLUNKENDE SUPERKONSOLL", hentOpprettetOppgave.data.melding.barn.navn)
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
        val hentOpprettetOppgave: TopicEntry<OppgaveOpprettet> = kafkaTestConsumer.hentOpprettetOppgave(melding.soknadId)
        assertEquals("KLØKTIG BLUNKENDE SUPERKONSOLL", hentOpprettetOppgave.data.melding.barn.navn)
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
        val hentOpprettetOppgave: TopicEntry<OppgaveOpprettet> = kafkaTestConsumer.hentOpprettetOppgave(melding.soknadId)
        assertEquals(forventetFodselsNummer, hentOpprettetOppgave.data.melding.barn.fodselsnummer)
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
        val hentOpprettetOppgave: TopicEntry<OppgaveOpprettet> = kafkaTestConsumer.hentOpprettetOppgave(melding.soknadId)
        assertEquals(forventetFodselsNummer, hentOpprettetOppgave.data.melding.barn.fodselsnummer)
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
        val hentOpprettetOppgave: TopicEntry<OppgaveOpprettet> = kafkaTestConsumer.hentOpprettetOppgave(melding.soknadId)
        assertEquals(LocalDate.now(), hentOpprettetOppgave.data.melding.barn.fodselsdato)
    }

    private fun gyldigMelding(
        fodselsnummerSoker: String,
        fodselsnummerBarn: String?,
        vedleggUrl : URI = URI("${wireMockServer.getK9DokumentBaseUrl()}/v1/dokument/${UUID.randomUUID()}"),
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
        )
    )

    private fun ventPaaAtRetryMekanismeIStreamProsessering() = runBlocking { delay(Duration.ofSeconds(30)) }
}