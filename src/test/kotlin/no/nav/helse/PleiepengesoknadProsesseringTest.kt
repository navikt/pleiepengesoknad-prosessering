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

import org.junit.AfterClass
import org.junit.BeforeClass
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.*

private val logger: Logger = LoggerFactory.getLogger("nav.PleiepengesoknadProsesseringTest")

@KtorExperimentalAPI
class PleiepengesoknadProsesseringTest {

    @KtorExperimentalAPI
    private companion object {

        private val wireMockServer: WireMockServer = WiremockWrapper.bootstrap()
        private val objectMapper = jacksonObjectMapper().dusseldorfConfigured()
        private val authorizedAccessToken = Authorization.getAccessToken(wireMockServer.baseUrl(), wireMockServer.getSubject())

        // Se https://github.com/navikt/dusseldorf-ktor#f%C3%B8dselsnummer
        private val gyldigFodselsnummerA = "02119970078"
        private val gyldigFodselsnummerB = "19066672169"
        private val gyldigFodselsnummerC = "20037473937"

        fun getConfig() : ApplicationConfig {
            val fileConfig = ConfigFactory.load()
            val testConfig = ConfigFactory.parseMap(TestConfiguration.asMap(wireMockServer = wireMockServer))
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

    }

    @Test
    fun `Request uten corelation id feiler`() {

    }

    @Test
    fun `En ugyldig melding gir valideringsfeil`() {
        val fraOgMedString = "2019-04-02"
        val tilOgMedString = "2019-04-01"
        val gyldigOrgnr = "917755736"
        val kortOrgnr = "123456"
        val ugyldigOrgnr = "987654321"
        val fodselsnummerSoker = "29099012345"
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
            )
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
                    }, {
                        "type": "entity",
                        "name": "fra_og_med",
                        "reason": "Fra og med må være før til og med.",
                        "invalid_value": "$fraOgMedString"
                    }, {
                        "type": "entity",
                        "name": "til_og_med",
                        "reason": "Til og med må være etter fra og med.",
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
                                 accessToken : String = authorizedAccessToken) {
        with(engine) {
            handleRequest(HttpMethod.Post, "/v1/soknad") {
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
                if (expectedResponse != null) {
                    JSONAssert.assertEquals(expectedResponse, response.content!!, true)
                } else {
                    assertEquals(expectedResponse, response.content)
                }

            }
        }
    }

    private fun gyldigMelding(
        fodselsnummerSoker : String,
        fodselsnummerBarn: String
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
        vedleggUrls = listOf(URL("http://localhost:8080/1234")),
        vedlegg = listOf(),
        medlemskap = Medlemskap(
            harBoddIUtlandetSiste12Mnd = true,
            skalBoIUtlandetNeste12Mnd = true
        )
    )
}