package no.nav.helse

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.application.ApplicationStopping
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.aktoer.AktoerGateway
import no.nav.helse.aktoer.AktoerService
import no.nav.helse.auth.AccessTokenClientResolver
import no.nav.helse.barn.BarnOppslag
import no.nav.helse.dokument.DokumentGateway
import no.nav.helse.dokument.DokumentService
import no.nav.helse.dusseldorf.ktor.auth.clients
import no.nav.helse.dusseldorf.ktor.client.HttpRequestHealthCheck
import no.nav.helse.dusseldorf.ktor.client.HttpRequestHealthConfig
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.helse.dusseldorf.ktor.core.Paths
import no.nav.helse.dusseldorf.ktor.core.logProxyProperties
import no.nav.helse.dusseldorf.ktor.health.HealthRoute
import no.nav.helse.dusseldorf.ktor.health.HealthService
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.dusseldorf.ktor.metrics.MetricsRoute
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.prosessering.v1.PdfV1Generator
import no.nav.helse.prosessering.v1.PreprosseseringV1Service
import no.nav.helse.prosessering.v1.asynkron.AsynkronProsesseringV1Service
import no.nav.helse.tpsproxy.TpsProxyV1Gateway
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

private val logger: Logger = LoggerFactory.getLogger("nav.PleiepengesoknadProsessering")

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
fun Application.pleiepengesoknadProsessering() {
    logProxyProperties()
    DefaultExports.initialize()

    install(ContentNegotiation) {
        jackson {
            pleiepengerKonfiguert()
        }
    }

    val configuration = Configuration(environment.config)

    val accessTokenClientResolver = AccessTokenClientResolver(environment.config.clients())

    val aktoerGateway = AktoerGateway(
        baseUrl = configuration.getAktoerRegisterBaseUrl(),
        accessTokenClient = accessTokenClientResolver.aktoerRegisterAccessTokenClient()
    )

    val aktoerService = AktoerService(aktoerGateway)

    val dokumentGateway = DokumentGateway(
        baseUrl = configuration.getK9DokumentBaseUrl(),
        accessTokenClient = accessTokenClientResolver.dokumentAccessTokenClient(),
        lagreDokumentScopes = configuration.getLagreDokumentScopes(),
        sletteDokumentScopes = configuration.getSletteDokumentScopes()
    )
    val dokumentService = DokumentService(dokumentGateway)

    val tpsProxyV1Gateway = TpsProxyV1Gateway(
        baseUrl = configuration.getTpsProxyV1Url(),
        accessTokenClient = accessTokenClientResolver.tpsProxyAccessTokenClient()
    )

    val preprosseseringV1Service = PreprosseseringV1Service(
        aktoerService = aktoerService,
        pdfV1Generator = PdfV1Generator(),
        dokumentService = dokumentService,
        barnOppslag = BarnOppslag(tpsProxyV1Gateway)
    )


    val joarkGateway = JoarkGateway(
        baseUrl = configuration.getk9JoarkBaseUrl(),
        accessTokenClient = accessTokenClientResolver.joarkAccessTokenClient(),
        journalforeScopes = configuration.getJournalforeScopes()
    )

    val asynkronProsesseringV1Service = AsynkronProsesseringV1Service(
        kafkaConfig = configuration.getKafkaConfig(),
        preprosseseringV1Service = preprosseseringV1Service,
        joarkGateway = joarkGateway,
        dokumentService = dokumentService
    )


    environment.monitor.subscribe(ApplicationStopping) {
        logger.info("Stopper AsynkronProsesseringV1Service.")
        asynkronProsesseringV1Service.stop()
        CollectorRegistry.defaultRegistry.clear()
        logger.info("AsynkronProsesseringV1Service Stoppet.")
    }

    install(Routing) {
        MetricsRoute()
        HealthRoute(
            path = Paths.DEFAULT_ALIVE_PATH,
            healthService = HealthService(
                healthChecks = asynkronProsesseringV1Service.isReadyChecks()
            )
        )
        get(Paths.DEFAULT_READY_PATH) {
            call.respondText("READY")
        }
        HealthRoute(
            healthService = HealthService(
                healthChecks = mutableSetOf(
                    dokumentGateway,
                    joarkGateway,
                    aktoerGateway,
                    HttpRequestHealthCheck(
                        mapOf(
                            Url.healthURL(configuration.getK9DokumentBaseUrl()) to HttpRequestHealthConfig(
                                expectedStatus = HttpStatusCode.OK
                            ),
                            Url.healthURL(configuration.getk9JoarkBaseUrl()) to HttpRequestHealthConfig(
                                expectedStatus = HttpStatusCode.OK
                            )
                        )
                    )
                )
                    .plus(asynkronProsesseringV1Service.healthChecks()).toSet()
            )
        )
    }
}

private fun Url.Companion.healthURL(baseUrl: URI) = Url.buildURL(baseUrl = baseUrl, pathParts = listOf("health"))

internal fun ObjectMapper.pleiepengerKonfiguert(): ObjectMapper = dusseldorfConfigured()
    .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
    .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
