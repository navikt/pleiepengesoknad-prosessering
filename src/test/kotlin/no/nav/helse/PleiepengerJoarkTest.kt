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
import no.nav.helse.journalforing.api.JournalforingResponse
import no.nav.helse.journalforing.v1.DokumentV1
import no.nav.helse.journalforing.v1.MeldingV1
import org.junit.AfterClass
import org.junit.BeforeClass
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import kotlin.test.*

private val logger: Logger = LoggerFactory.getLogger("nav.PleiepengerJoarkTest")

@KtorExperimentalAPI
class PleiepengerJoarkTest {

    @KtorExperimentalAPI
    private companion object {

        val wireMockServer: WireMockServer = WiremockWrapper.bootstrap()
        val objectMapper = ObjectMapper.server()

        fun getConfig() : ApplicationConfig {
            val fileConfig = ConfigFactory.load()
            val testConfig = ConfigFactory.parseMap(mutableMapOf(
                Pair("nav.authorization.jwks_url", wireMockServer.getJwksUrl()),
                Pair("nav.authorization.token_url", wireMockServer.getTokenUrl()),
                Pair("nav.joark.inngaaende_forsendelse_url", wireMockServer.getJoarkInngaaendeForsendelseUrl())
            ))

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
    fun `test isready og isalive`() {
        with(engine) {
            handleRequest(HttpMethod.Get, "/isready") {}.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                handleRequest(HttpMethod.Get, "/isalive") {}.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                }
            }
        }
    }

    @Test
    fun `gyldig melding til joark gir ok response med jorunalpost ID`() {
        val authorizationHeader = Authorization.getAuthorizationHeader()
        val request = MeldingV1(
            tittel = "Dette er tittelen",
            aktoerId = "1234",
            soknadId = "5678",
            mottatt = ZonedDateTime.now(),
            dokumenter = listOf(
                DokumentV1(
                    tittel = "Hoveddokument",
                    innhold = "test.pdf".fromResources(),
                    contentType = "application/pdf"
                )
            )
        )
        val expectedResponse = JournalforingResponse(journalPostId = "1234")

        with(engine) {
            handleRequest(HttpMethod.Post, "/v1/journalforing") {
                addHeader(HttpHeaders.Authorization, authorizationHeader)
                addHeader("Nav-Call-Id", "123156")
                setBody(objectMapper.writeValueAsString(request))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(expectedResponse, objectMapper.readValue(response.content!!))
            }
        }
    }

    private fun String.fromResources() : ByteArray {
        return Thread.currentThread().contextClassLoader.getResource(this).readBytes()
    }
}