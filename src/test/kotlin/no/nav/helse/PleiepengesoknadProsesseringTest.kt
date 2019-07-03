package no.nav.helse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.typesafe.config.ConfigFactory
import io.ktor.config.ApplicationConfig
import io.ktor.config.HoconApplicationConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.createTestEnvironment
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.prosessering.v1.*
import no.nav.helse.prosessering.v1.asynkron.OppgaveOpprettet
import no.nav.helse.prosessering.v1.asynkron.TopicEntry
import no.nav.helse.prosessering.v1.asynkron.Topics
import no.nav.helse.prosessering.v1.asynkron.Topics.OPPGAVE_OPPRETTET
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.json.JSONObject

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
import kotlin.test.*

private val logger: Logger = LoggerFactory.getLogger("nav.PleiepengesoknadProsesseringTest")

@KtorExperimentalAPI
class PleiepengesoknadProsesseringTest {

    @KtorExperimentalAPI
    private companion object {

        private val wireMockServer: WireMockServer = WiremockWrapper.bootstrap()
        private val kafkaEnvironment = KafkaWrapper.bootstrap()
        private val objectMapper = jacksonObjectMapper().dusseldorfConfigured()
        private val authorizedAccessToken = Authorization.getAccessToken(wireMockServer.baseUrl(), wireMockServer.getSubject())
        private val unAauthorizedAccessToken = Authorization.getAccessToken(wireMockServer.baseUrl(), "srvikketilgang")

        // Se https://github.com/navikt/dusseldorf-ktor#f%C3%B8dselsnummer
        private val gyldigFodselsnummerA = "02119970078"
        private val gyldigFodselsnummerB = "19066672169"
        private val gyldigFodselsnummerC = "20037473937"
        private val dNummerA = "55125314561"


        fun getConfig() : ApplicationConfig {
            val fileConfig = ConfigFactory.load()
            val testConfig = ConfigFactory.parseMap(TestConfiguration.asMap(wireMockServer = wireMockServer, kafkaEnvironment = kafkaEnvironment))
            val mergedConfig = testConfig.withFallback(fileConfig)
            return HoconApplicationConfig(mergedConfig)
        }


        val engine = TestApplicationEngine(createTestEnvironment {
            config = getConfig()
        })


        @BeforeClass
        @JvmStatic
        fun buildUp() {
            engine.start(wait = true)
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            logger.info("Tearing down")
            wireMockServer.stop()
            engine.stop(10, 5, TimeUnit.SECONDS)
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
    fun `Gylding melding blir lagt til prosessering`() {
        val melding = gyldigMelding(
            fodselsnummerSoker = gyldigFodselsnummerA,
            fodselsnummerBarn = gyldigFodselsnummerB
        )

        WiremockWrapper.stubAktoerRegisterGetAktoerId(gyldigFodselsnummerA, "12121212")
        WiremockWrapper.stubAktoerRegisterGetAktoerId(gyldigFodselsnummerB, "23232323")

        requestAndAssert(
            request = melding,
            expectedCode = HttpStatusCode.Accepted,
            expectedResponse = null
        )
    }

    @Test
    fun `Gylding melding blir lagt til prosessering asynkront`() {
        val melding = gyldigMelding(
            fodselsnummerSoker = gyldigFodselsnummerA,
            fodselsnummerBarn = gyldigFodselsnummerB
        )

        WiremockWrapper.stubAktoerRegisterGetAktoerId(gyldigFodselsnummerA, "121212166")
        WiremockWrapper.stubAktoerRegisterGetAktoerId(gyldigFodselsnummerB, "232323267")

        requestAndAssert(
            request = melding,
            expectedCode = HttpStatusCode.Accepted,
            expectedResponse = null,
            async = true
        )

        ventPaaOppgaveOpprettet()
    }

    @Test
    fun `Melding som gjeder søker med D-nummer`() {
        val melding = gyldigMelding(
            fodselsnummerSoker = dNummerA,
            fodselsnummerBarn = gyldigFodselsnummerB
        )

        WiremockWrapper.stubAktoerRegisterGetAktoerId(dNummerA, "12121255")
        WiremockWrapper.stubAktoerRegisterGetAktoerId(gyldigFodselsnummerB, "23232356")

        requestAndAssert(
            request = melding,
            expectedCode = HttpStatusCode.Accepted,
            expectedResponse = null
        )
    }

    @Test
    fun `Melding lagt til prosessering selv om sletting av vedlegg feiler`() {
        val melding = gyldigMelding(
            fodselsnummerSoker = gyldigFodselsnummerA,
            fodselsnummerBarn = gyldigFodselsnummerB,
            vedleggUrl = URI("http://localhost:8080/jeg-skal-feile/1")
        )

        WiremockWrapper.stubAktoerRegisterGetAktoerId(gyldigFodselsnummerA, "12121212")
        WiremockWrapper.stubAktoerRegisterGetAktoerId(gyldigFodselsnummerB, "23232323")

        requestAndAssert(
            request = melding,
            expectedCode = HttpStatusCode.Accepted,
            expectedResponse = null
        )
    }

    @Test
    fun `Melding lagt til prosessering selv om oppslag paa aktoer ID for barn feiler`() {
        val melding = gyldigMelding(
            fodselsnummerSoker = gyldigFodselsnummerA,
            fodselsnummerBarn = gyldigFodselsnummerC
        )

        WiremockWrapper.stubAktoerRegisterGetAktoerId(gyldigFodselsnummerA, "12121212")
        WiremockWrapper.stubAktoerRegisterGetAktoerIdNotFound(gyldigFodselsnummerC)

        requestAndAssert(
            request = melding,
            expectedCode = HttpStatusCode.Accepted,
            expectedResponse = null
        )
    }

    @Test
    fun `Request fra ikke autorisert system feiler`() {
        val melding = gyldigMelding(
            fodselsnummerSoker = gyldigFodselsnummerA,
            fodselsnummerBarn = gyldigFodselsnummerB
        )

        requestAndAssert(
            request = melding,
            expectedCode = HttpStatusCode.Forbidden,
            expectedResponse = """
            {
                "type": "/problem-details/unauthorized",
                "title": "unauthorized",
                "status": 403,
                "detail": "Requesten inneholder ikke tilstrekkelige tilganger.",
                "instance": "about:blank"
            }
            """.trimIndent(),
            accessToken = unAauthorizedAccessToken
        )
    }

    @Test
    fun `Request uten corelation id feiler`() {
        val melding = gyldigMelding(
            fodselsnummerSoker = gyldigFodselsnummerA,
            fodselsnummerBarn = gyldigFodselsnummerB
        )

        requestAndAssert(
            request = melding,
            expectedCode = HttpStatusCode.BadRequest,
            expectedResponse = """
                {
                    "type": "/problem-details/invalid-request-parameters",
                    "title": "invalid-request-parameters",
                    "detail": "Requesten inneholder ugyldige paramtere.",
                    "status": 400,
                    "instance": "about:blank",
                    "invalid_parameters" : [
                        {
                            "name" : "X-Correlation-ID",
                            "reason" : "Correlation ID må settes.",
                            "type": "header",
                            "invalid_value": null
                        }
                    ]
                }
            """.trimIndent(),
            leggTilCorrelationId = false
        )
    }

    @Test
    fun `En ugyldig melding gir valideringsfeil`() {
        val fraOgMedString = "2019-04-02"
        val tilOgMedString = "2019-04-01"
        val gyldigOrgnr = "917755736"
        val kortOrgnr = "123456"
        val ugyldigOrgnr = "987654321"
        val fodselsnummerSoker = "290990123451"
        val fodselsnummerBarn = "29099054321"
        val alternativIdBarn = "123F"

        val melding = MeldingV1(
            mottatt = ZonedDateTime.now(),
            fraOgMed = LocalDate.parse(fraOgMedString),
            tilOgMed = LocalDate.parse(tilOgMedString),
            soker = Soker(
                fodselsnummer = fodselsnummerSoker,
                etternavn = "Nordmann",
                mellomnavn = "Mellomnavn",
                fornavn = "Ola"
             ),
            barn = Barn(
                navn = "Kari",
                fodselsnummer = fodselsnummerBarn,
                alternativId = alternativIdBarn
            ),
            relasjonTilBarnet = "Mor",
            arbeidsgivere = Arbeidsgivere(
                organisasjoner = listOf(
                    Organisasjon(gyldigOrgnr, "Gyldig"),
                    Organisasjon(kortOrgnr, "ForKort"),
                    Organisasjon(ugyldigOrgnr, "Ugyldig")
                )
            ),
            vedleggUrls = listOf(),
            vedlegg = listOf(),
            medlemskap = Medlemskap(
                harBoddIUtlandetSiste12Mnd = true,
                skalBoIUtlandetNeste12Mnd = true
            ),
            harMedsoker = true,
            grad = 120,
            harBekreftetOpplysninger = false,
            harForstattRettigheterOgPlikter = false
        )
        requestAndAssert(
            request = melding,
            expectedCode = HttpStatusCode.BadRequest,
            expectedResponse = """
                {
                    "type": "/problem-details/invalid-request-parameters",
                    "title": "invalid-request-parameters",
                    "status": 400,
                    "detail": "Requesten inneholder ugyldige paramtere.",
                    "instance": "about:blank",
                    "invalid_parameters": [{
                        "type": "entity",
                        "name": "vedlegg",
                        "reason": "Det må sendes minst et vedlegg eller en vedlegg URL.",
                        "invalid_value": []
                    }, {
                        "type": "entity",
                        "name": "vedlegg_urls",
                        "reason": "Det må sendes minst et vedlegg eller en vedlegg URL.",
                        "invalid_value": []
                    }, {
                        "type": "entity",
                        "name": "soker.fodselsnummer",
                        "reason": "Ikke gyldig fødselsnummer.",
                        "invalid_value": "$fodselsnummerSoker"
                    }, {
                        "type": "entity",
                        "name": "barn.fodselsnummer",
                        "reason": "Ikke gyldig fødselsnummer.",
                        "invalid_value": "$fodselsnummerBarn"
                    }, {
                        "type": "entity",
                        "name": "barn.alternativ_id",
                        "reason": "Ikke gyldig alternativ id. Kan kun inneholde tall.",
                        "invalid_value": "$alternativIdBarn"
                    }, {
                        "type": "entity",
                        "name": "arbeidsgivere.organisasjoner[1].organisasjonsnummer",
                        "reason": "Ikke gyldig organisasjonsnummer.",
                        "invalid_value": "$kortOrgnr"
                    }, {
                        "type": "entity",
                        "name": "arbeidsgivere.organisasjoner[2].organisasjonsnummer",
                        "reason": "Ikke gyldig organisasjonsnummer.",
                        "invalid_value": "$ugyldigOrgnr"
                    },{
                        "type": "entity",
                        "name": "har_bekreftet_opplysninger",
                        "reason": "Opplysningene må bekreftes for å legge søknad til prosessering.",
                        "invalid_value": false
                    },{
                        "type": "entity",
                        "name": "har_forstatt_rettigheter_og_plikter",
                        "reason": "Må ha forstått rettigheter og plikter for å legge søknad til prosessering.",
                        "invalid_value": false
                    },{
                        "type": "entity",
                        "name": "grad",
                        "reason": "Grad må være mellom 20 og 100.",
                        "invalid_value": 120
                    },{
                        "type": "entity",
                        "name": "fra_og_med",
                        "reason": "Fra og med må være før eller lik til og med.",
                        "invalid_value": "$fraOgMedString"
                    }, {
                        "type": "entity",
                        "name": "til_og_med",
                        "reason": "Til og med må være etter eller lik fra og med.",
                        "invalid_value": "$tilOgMedString"
                    }]
                }
            """.trimIndent()
        )
    }

    private fun requestAndAssert(request : MeldingV1,
                                 expectedResponse : String?,
                                 expectedCode : HttpStatusCode,
                                 leggTilCorrelationId : Boolean = true,
                                 leggTilAuthorization : Boolean = true,
                                 accessToken : String = authorizedAccessToken,
                                 async: Boolean = false) {
        with(engine) {
            handleRequest(HttpMethod.Post, "/v1/soknad${if (async) "?async=true" else ""}") {
                if (leggTilAuthorization) {
                    addHeader(HttpHeaders.Authorization, "Bearer $accessToken")
                }
                if (leggTilCorrelationId) {
                    addHeader(HttpHeaders.XCorrelationId, "123156")
                }
                addHeader(HttpHeaders.ContentType, "application/json")
                val requestEntity = objectMapper.writeValueAsString(request)
                logger.info("Request Entity = $requestEntity")
                setBody(objectMapper.writeValueAsString(request))
            }.apply {
                logger.info("Response Entity = ${response.content}")
                logger.info("Expected Entity = $expectedResponse")
                assertEquals(expectedCode, response.status())
                when {
                    expectedResponse != null -> JSONAssert.assertEquals(expectedResponse, response.content!!, true)
                    HttpStatusCode.Accepted == response.status() -> {
                        val json = JSONObject(response.content!!)
                        assertEquals(1, json.keySet().size)
                        assertNotNull(json.getString("id"))
                    }
                    else -> assertEquals(expectedResponse, response.content)
                }

            }
        }
    }

    private fun gyldigMelding(
        fodselsnummerSoker : String,
        fodselsnummerBarn: String,
        vedleggUrl : URI = URI("${wireMockServer.getPleiepengerDokumentBaseUrl()}/v1/dokument/${UUID.randomUUID()}")
    ) : MeldingV1 = MeldingV1(
        mottatt = ZonedDateTime.now(),
        fraOgMed = LocalDate.now(),
        tilOgMed = LocalDate.now().plusWeeks(1),
        soker = Soker(
            fodselsnummer = fodselsnummerSoker,
            etternavn = "Nordmann",
            mellomnavn = "Mellomnavn",
            fornavn = "Ola"
        ),
        barn = Barn(
            navn = "Kari",
            fodselsnummer = fodselsnummerBarn,
            alternativId = null
        ),
        relasjonTilBarnet = "Mor",
        arbeidsgivere = Arbeidsgivere(
            organisasjoner = listOf(
                Organisasjon("917755736", "Gyldig")
            )
        ),
        vedleggUrls = listOf(vedleggUrl),
        vedlegg = listOf(),
        medlemskap = Medlemskap(
            harBoddIUtlandetSiste12Mnd = true,
            skalBoIUtlandetNeste12Mnd = true
        ),
        harMedsoker = true,
        grad = 70,
        harBekreftetOpplysninger = true,
        harForstattRettigheterOgPlikter = true
    )

    private fun ventPaaOppgaveOpprettet() : TopicEntry<OppgaveOpprettet> {
        val consumer = KafkaConsumer<String, TopicEntry<OppgaveOpprettet>>(
            kafkaEnvironment.testConsumerProperties(),
            StringDeserializer(),
            Topics.OPPGAVE_OPPRETTET.serDes
        )
        consumer.subscribe(listOf(OPPGAVE_OPPRETTET.name))

        val end = System.currentTimeMillis() + Duration.ofSeconds(20).toMillis()
        while (System.currentTimeMillis() < end) {
            val records = consumer.poll(Duration.ofSeconds(1))
            if (!records.isEmpty) {
                assertEquals(1, records.count())

                return records.records(OPPGAVE_OPPRETTET.name).map {
                    it.value()
                }.first()

                consumer.commitSync()
            }
        }
        throw IllegalStateException("Fant ikke opprettet oppgae etter 20 sekunder.")
    }
}