package no.nav.helse

import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.features.*
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.jackson.jackson
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.aktoer.AktoerGateway
import no.nav.helse.aktoer.AktoerService
import no.nav.helse.auth.AccessTokenClientResolver
import no.nav.helse.dokument.DokumentGateway
import no.nav.helse.dokument.DokumentService
import no.nav.helse.dusseldorf.ktor.auth.*
import no.nav.helse.dusseldorf.ktor.client.*
import no.nav.helse.dusseldorf.ktor.core.*
import no.nav.helse.dusseldorf.ktor.health.HealthRoute
import no.nav.helse.dusseldorf.ktor.health.HealthService
import no.nav.helse.dusseldorf.ktor.jackson.JacksonStatusPages
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.dusseldorf.ktor.metrics.MetricsRoute
import no.nav.helse.dusseldorf.ktor.metrics.init
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.oppgave.OppgaveGateway
import no.nav.helse.prosessering.api.prosesseringApis
import no.nav.helse.prosessering.v1.PdfV1Generator
import no.nav.helse.prosessering.v1.PreprosseseringV1Service
import no.nav.helse.prosessering.v1.asynkron.AsynkronProsesseringV1Service
import no.nav.helse.prosessering.v1.synkron.SynkronProsesseringV1Service
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

private val logger: Logger = LoggerFactory.getLogger("nav.PleiepengesoknadProsessering")

fun main(args: Array<String>): Unit  = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
fun Application.pleiepengesoknadProsessering() {
    val appId = environment.config.id()
    logProxyProperties()
    DefaultExports.initialize()

    val configuration = Configuration(environment.config)
    val issuers = configuration.issuers()
    val kafkaConfig = configuration.getKafkaConfig()

    install(Authentication) {
        multipleJwtIssuers(issuers)
    }

    install(ContentNegotiation) {
        jackson {
            dusseldorfConfigured()
        }
    }

    install(StatusPages) {
        DefaultStatusPages()
        JacksonStatusPages()
        AuthStatusPages()
    }

    install(CallIdRequired)

    val accessTokenClientResolver = AccessTokenClientResolver(environment.config.clients())
    val aktoerService = AktoerService(
        aktoerGateway = AktoerGateway(
            baseUrl = configuration.getAktoerRegisterBaseUrl(),
            accessTokenClient = accessTokenClientResolver.aktoerRegisterAccessTokenClient()
        )
    )
    val dokumentService = DokumentService(
        dokumentGateway = DokumentGateway(
            baseUrl = configuration.getPleiepengerDokumentBaseUrl(),
            accessTokenClient = accessTokenClientResolver.dokumentAccessTokenClient()
        )
    )
    val preprosseseringV1Service = PreprosseseringV1Service(
        aktoerService = aktoerService,
        pdfV1Generator = PdfV1Generator(),
        dokumentService = dokumentService
    )
    val joarkGateway = JoarkGateway(
        baseUrl = configuration.getPleiepengerJoarkBaseUrl(),
        accessTokenClient = accessTokenClientResolver.joarkAccessTokenClient()
    )

    val oppgaveGateway = OppgaveGateway(
        baseUrl = configuration.getPleiepengerOppgaveBaseUrl(),
        accessTokenClient = accessTokenClientResolver.oppgaveAccessTokenClient()
    )

    val asynkronProsesseringV1Service = kafkaConfig?.let { config ->
        AsynkronProsesseringV1Service(
            kafkaConfig = config,
            preprosseseringV1Service = preprosseseringV1Service,
            joarkGateway = joarkGateway,
            oppgaveGateway = oppgaveGateway,
            dokumentService = dokumentService
        )
    }

    if (asynkronProsesseringV1Service != null) logger.info("Prosesserer søknader asynkront.")
    else logger.info("Prosesserer søknader synkront.")

    val prosesseringV1Service = asynkronProsesseringV1Service ?: SynkronProsesseringV1Service(
        preprosseseringV1Service = preprosseseringV1Service,
        joarkGateway = joarkGateway,
        oppgaveGateway = oppgaveGateway
    )



    environment.monitor.subscribe(ApplicationStopping) {
        logger.info("Stopper AsynkronProsesseringV1Service.")
        asynkronProsesseringV1Service?.stop()
        logger.info("AsynkronProsesseringV1Service Stoppet.")
    }

    install(Routing) {
        authenticate(*issuers.allIssuers()) {
            requiresCallId {
                prosesseringApis(
                    prosesseringV1Service = prosesseringV1Service,
                    aktoerService = aktoerService,
                    dokumentService = dokumentService
                )
            }
        }
        MetricsRoute()
        HealthRoute(
            path = Paths.DEFAULT_ALIVE_PATH,
            healthService = HealthService(
                healthChecks = asynkronProsesseringV1Service?.isReadyChecks() ?: emptySet()
            )
        )
        get(Paths.DEFAULT_READY_PATH) {
            call.respondText("READY")
        }
        HealthRoute(
            healthService = HealthService(
                healthChecks = mutableSetOf(
                    accessTokenClientResolver,
                    HttpRequestHealthCheck(issuers.healthCheckMap(mutableMapOf(
                        Url.healthURL(configuration.getPleiepengerDokumentBaseUrl()) to HttpRequestHealthConfig(expectedStatus = HttpStatusCode.OK),
                        Url.healthURL(configuration.getPleiepengerJoarkBaseUrl()) to HttpRequestHealthConfig(expectedStatus = HttpStatusCode.OK),
                        Url.healthURL(configuration.getPleiepengerOppgaveBaseUrl()) to HttpRequestHealthConfig(expectedStatus = HttpStatusCode.OK)
                    )))
                ).plus(asynkronProsesseringV1Service?.healthChecks()?: emptySet()).toSet()
            )
        )
    }

    install(MicrometerMetrics) {
        init(appId)
    }

    intercept(ApplicationCallPipeline.Monitoring) {
        call.request.log()
    }

    install(CallId) {
        fromXCorrelationIdHeader()
    }

    install(CallLogging) {
        correlationIdAndRequestIdInMdc()
        logRequests()
    }
}
private fun Url.Companion.healthURL(baseUrl: URI) = Url.buildURL(baseUrl = baseUrl, pathParts = listOf("health"))

private fun Map<Issuer, Set<ClaimRule>>.healthCheckMap(
    initial : MutableMap<URI, HttpRequestHealthConfig>
) : Map<URI, HttpRequestHealthConfig> {
    forEach { issuer, _ ->
        initial[issuer.jwksUri()] = HttpRequestHealthConfig(expectedStatus = HttpStatusCode.OK, includeExpectedStatusEntity = false)
    }
    return initial.toMap()
}
