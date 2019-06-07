package no.nav.helse

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.*
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.jackson.jackson
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.routing.Routing
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.aktoer.AktoerGateway
import no.nav.helse.aktoer.AktoerService
import no.nav.helse.auth.AccessTokenClientResolver
import no.nav.helse.dokument.DokumentGateway
import no.nav.helse.dokument.DokumentService
import no.nav.helse.dusseldorf.ktor.auth.AuthStatusPages
import no.nav.helse.dusseldorf.ktor.auth.clients
import no.nav.helse.dusseldorf.ktor.client.*
import no.nav.helse.dusseldorf.ktor.core.*
import no.nav.helse.dusseldorf.ktor.health.HealthRoute
import no.nav.helse.dusseldorf.ktor.health.HealthService
import no.nav.helse.dusseldorf.ktor.jackson.JacksonStatusPages
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.dusseldorf.ktor.metrics.MetricsRoute
import no.nav.helse.dusseldorf.ktor.metrics.init
import no.nav.helse.gosys.GosysService
import no.nav.helse.gosys.JoarkGateway
import no.nav.helse.gosys.OppgaveGateway
import no.nav.helse.prosessering.api.prosesseringApis
import no.nav.helse.prosessering.v1.PdfV1Generator
import no.nav.helse.prosessering.v1.ProsesseringV1Service
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.TimeUnit

private val logger: Logger = LoggerFactory.getLogger("nav.PleiepengesoknadProsessering")

fun main(args: Array<String>): Unit  = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
fun Application.pleiepengesoknadProsessering() {
    val appId = environment.config.id()
    logProxyProperties()
    DefaultExports.initialize()

    val configuration = Configuration(environment.config)

    val authorizedSystems = configuration.getAuthorizedSystemsForRestApi()

    val jwkProvider = JwkProviderBuilder(configuration.getJwksUrl().toURL())
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    install(Authentication) {
        jwt {
            verifier(jwkProvider, configuration.getIssuer())
            realm = appId
            validate { credentials ->
                log.info("authorization attempt for ${credentials.payload.subject}")
                if (credentials.payload.subject in authorizedSystems) {
                    log.info("authorization ok")
                    return@validate JWTPrincipal(credentials.payload)
                }
                log.warn("authorization failed")
                return@validate null
            }
        }
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

    install(Routing) {
        authenticate {
            requiresCallId {
                prosesseringApis(
                    prosesseringV1Service = ProsesseringV1Service(
                        gosysService = GosysService(
                            joarkGateway = JoarkGateway(
                                baseUrl = configuration.getPleiepengerJoarkBaseUrl(),
                                accessTokenClient = accessTokenClientResolver.joarkAccessTokenClient()
                            ),
                            oppgaveGateway = OppgaveGateway(
                                baseUrl = configuration.getPleiepengerOppgaveBaseUrl(),
                                accessTokenClient = accessTokenClientResolver.oppgaveAccessTokenClient()
                            )
                        ),
                        aktoerService = AktoerService(
                            aktoerGateway = AktoerGateway(
                                baseUrl = configuration.getAktoerRegisterBaseUrl(),
                                accessTokenClient = accessTokenClientResolver.aktoerRegisterAccessTokenClient()
                            )
                        ),
                        pdfV1Generator = PdfV1Generator(),
                        dokumentService = DokumentService(
                            dokumentGateway = DokumentGateway(
                                baseUrl = configuration.getPleiepengerDokumentBaseUrl(),
                                accessTokenClient = accessTokenClientResolver.dokumentAccessTokenClient()
                            )
                        )
                    )
                )
            }
        }
        DefaultProbeRoutes()
        MetricsRoute()
        HealthRoute(
            healthService = HealthService(
                healthChecks = setOf(
                    accessTokenClientResolver,
                    HttpRequestHealthCheck(mapOf(
                        configuration.getJwksUrl() to HttpRequestHealthConfig(expectedStatus = HttpStatusCode.OK, includeExpectedStatusEntity = false),
                        Url.healthURL(configuration.getPleiepengerDokumentBaseUrl()) to HttpRequestHealthConfig(expectedStatus = HttpStatusCode.OK),
                        Url.healthURL(configuration.getPleiepengerJoarkBaseUrl()) to HttpRequestHealthConfig(expectedStatus = HttpStatusCode.OK),
                        Url.healthURL(configuration.getPleiepengerOppgaveBaseUrl()) to HttpRequestHealthConfig(expectedStatus = HttpStatusCode.OK)
                    ))
                )
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