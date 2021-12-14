package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.server.testing.*
import no.nav.helse.dusseldorf.testsupport.asArguments
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class PleiepengesoknadProsesseringWithMocks {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(PleiepengesoknadProsesseringWithMocks::class.java)

        @JvmStatic
        fun main(args: Array<String>) {

            val wireMockServer: WireMockServer = WireMockBuilder()
                .withPort(8091)
                .withNaisStsSupport()
                .withAzureSupport()
                .build()
                .stubK9MellomlagringHealth()
                .stubPleiepengerJoarkHealth()
                .stubJournalfor()
                .stubLagreDokument()
                .stubSlettDokument()

            val kafkaEnvironment = KafkaWrapper.bootstrap()

            val testArgs = TestConfiguration.asMap(
                wireMockServer = wireMockServer,
                kafkaEnvironment = kafkaEnvironment,
                port = 8092
            ).asArguments()

            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    logger.info("Tearing down")
                    wireMockServer.stop()
                    kafkaEnvironment.tearDown()
                    logger.info("Tear down complete")
                }
            })
            withApplication { no.nav.helse.main(testArgs) }
        }
    }
}
