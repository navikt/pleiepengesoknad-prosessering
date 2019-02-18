package no.nav.helse

import com.fasterxml.jackson.module.kotlin.readValue
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
import no.nav.helse.sak.api.ManglerCorrelationId
import no.nav.helse.sak.api.SakResponse
import no.nav.helse.sak.v1.MeldingV1
import no.nav.helse.validering.Valideringsfeil
import org.junit.AfterClass
import org.junit.BeforeClass
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.*

private val logger: Logger = LoggerFactory.getLogger("nav.PleiepengerSakTest")

@KtorExperimentalAPI
class PleiepengerJoarkTest {

    @KtorExperimentalAPI
    private companion object {

        private val wireMockServer: WireMockServer = WiremockWrapper.bootstrap()
        private val objectMapper = ObjectMapper.server()
        private val accessToken = Authorization.getAccessToken(wireMockServer.baseUrl(), wireMockServer.getSubject())

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
    fun `test isready, isalive og metrics`() {
        with(engine) {
            handleRequest(HttpMethod.Get, "/isready") {}.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                handleRequest(HttpMethod.Get, "/isalive") {}.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    handleRequest(HttpMethod.Get, "/metrics") {}.apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                    }
                }
            }
        }
    }

    @Test
    fun `gyldig melding til sak gir ok response med en sak ID`() {
        val aktoerId = "123456"
        val sakId =  "56789"

        WiremockWrapper.stubSakOk(sakId = sakId, aktoerId = aktoerId)

        val request = MeldingV1(
            aktoerId = aktoerId
        )

        val expectedResponse = SakResponse(sakId = sakId)

        requestAndAssert(
            request = request,
            expectedCode = HttpStatusCode.Created,
            expectedResponse = expectedResponse
        )
    }

    @Test(expected = Valideringsfeil::class)
    fun `ugylidig melding gir valideringsfeil foer sak blir forsoekt opprettet`() {
        requestAndAssert(
            request = MeldingV1(
                aktoerId = "456456F44"
            )
        )
    }

    @Test(expected = ManglerCorrelationId::class)
    fun `request uten correlation id feiler foer sak blir forsoekt opprettet`() {
        requestAndAssert(
            request = MeldingV1(
                aktoerId = "123456"
            ),
            medCorrelationId = false
        )
    }

    private fun requestAndAssert(request : MeldingV1,
                                 expectedResponse : SakResponse? = null,
                                 expectedCode : HttpStatusCode? = null,
                                 medCorrelationId : Boolean = true) {
        with(engine) {
            handleRequest(HttpMethod.Post, "/v1/sak") {
                addHeader(HttpHeaders.Authorization, "Bearer $accessToken")
                if (medCorrelationId) {
                    addHeader(HttpHeaders.XCorrelationId, "123156")
                }
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody(objectMapper.writeValueAsString(request))
            }.apply {
                assertEquals(expectedCode, response.status())
                assertEquals(expectedResponse, objectMapper.readValue(response.content!!))
            }
        }
    }
}