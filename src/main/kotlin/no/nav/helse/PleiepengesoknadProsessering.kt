package no.nav.helse

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.Application
import io.ktor.application.ApplicationStopping
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.jackson.jackson
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.aktoer.AktoerGateway
import no.nav.helse.aktoer.AktoerService
import no.nav.helse.auth.AccessTokenClientResolver
import no.nav.helse.auth.NaisStsAccessTokenClient
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
import no.nav.helse.oppgave.OppgaveGateway
import no.nav.helse.prosessering.v1.PdfV1Generator
import no.nav.helse.prosessering.v1.PreprosseseringV1Service
import no.nav.helse.prosessering.v1.asynkron.AsynkronProsesseringV1Service
import no.nav.helse.tpsproxy.TpsProxyV1
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
            dusseldorfConfigured().configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)

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

    val naisStsAccessTokenClient = NaisStsAccessTokenClient(
        tokenEndpoint = configuration.getRestTokenUrl(),
        clientId = configuration.getClientId(),
        clientSecret = configuration.getClientSecret()
    )

    val tpsProxyV1Gateway = TpsProxyV1Gateway(
        tpsProxyV1 = TpsProxyV1(
            baseUrl = configuration.getTpsProxyV1Url(),
            accessTokenClient = naisStsAccessTokenClient
        )
    )

    val preprosseseringV1Service = PreprosseseringV1Service(
        aktoerService = aktoerService,
        pdfV1Generator = PdfV1Generator(),
        dokumentService = dokumentService,
        barnOppslag = BarnOppslag(tpsProxyV1Gateway)
    )
    val joarkGateway = JoarkGateway(
        baseUrl = configuration.getPleiepengerJoarkBaseUrl(),
        accessTokenClient = accessTokenClientResolver.joarkAccessTokenClient(),
        journalforeScopes = configuration.getJournalforeScopes()
    )

    val oppgaveGateway = OppgaveGateway(
        baseUrl = configuration.getPleiepengerOppgaveBaseUrl(),
        accessTokenClient = accessTokenClientResolver.oppgaveAccessTokenClient(),
        oppretteOppgaveScopes = configuration.getOppretteOppgaveScopes()
    )

    val asynkronProsesseringV1Service = AsynkronProsesseringV1Service(
        kafkaConfig = configuration.getKafkaConfig(),
        preprosseseringV1Service = preprosseseringV1Service,
        joarkGateway = joarkGateway,
        oppgaveGateway = oppgaveGateway,
        dokumentService = dokumentService
    )

    environment.monitor.subscribe(ApplicationStopping) {
        logger.info("Stopper AsynkronProsesseringV1Service.")
        asynkronProsesseringV1Service.stop()
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
                    oppgaveGateway,
                    aktoerGateway,
                    HttpRequestHealthCheck(
                        mapOf(
                            Url.healthURL(configuration.getK9DokumentBaseUrl()) to HttpRequestHealthConfig(expectedStatus = HttpStatusCode.OK),
                            Url.healthURL(configuration.getPleiepengerJoarkBaseUrl()) to HttpRequestHealthConfig(expectedStatus = HttpStatusCode.OK),
                            Url.healthURL(configuration.getPleiepengerOppgaveBaseUrl()) to HttpRequestHealthConfig(expectedStatus = HttpStatusCode.OK)
                        )
                    )
                ).plus(asynkronProsesseringV1Service.healthChecks()).toSet()
            )
        )
    }
}

private fun Url.Companion.healthURL(baseUrl: URI) = Url.buildURL(baseUrl = baseUrl, pathParts = listOf("health"))