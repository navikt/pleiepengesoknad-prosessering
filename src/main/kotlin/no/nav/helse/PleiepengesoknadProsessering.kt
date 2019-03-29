package no.nav.helse

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.Logging
import io.ktor.features.*
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.routing.Routing
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.aktoer.AktoerGateway
import no.nav.helse.aktoer.AktoerService
import no.nav.helse.dokument.DokumentGateway
import no.nav.helse.dokument.DokumentService
import no.nav.helse.dusseldorf.ktor.client.*
import no.nav.helse.dusseldorf.ktor.core.*
import no.nav.helse.dusseldorf.ktor.health.HealthRoute
import no.nav.helse.dusseldorf.ktor.jackson.JacksonStatusPages
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.dusseldorf.ktor.metrics.MetricsRoute
import no.nav.helse.gosys.GosysService
import no.nav.helse.gosys.JoarkGateway
import no.nav.helse.gosys.OppgaveGateway
import no.nav.helse.prosessering.api.prosesseringApis
import no.nav.helse.prosessering.v1.PdfV1Generator
import no.nav.helse.prosessering.v1.ProsesseringV1Service
import no.nav.helse.systembruker.SystembrukerGateway
import no.nav.helse.systembruker.SystembrukerService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private val logger: Logger = LoggerFactory.getLogger("nav.PleiepengesoknadProsessering")

fun main(args: Array<String>): Unit  = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
fun Application.pleiepengesoknadProsessering() {
    val appId = environment.config.id()
    logProxyProperties()
    DefaultExports.initialize()

    val httpClient = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = JacksonSerializer{
                ObjectMapper.server(this)
            }
        }
        engine {
            customizeClient { setProxyRoutePlanner() }
        }
    }

    val configuration = Configuration(environment.config)

    val authorizedSystems = configuration.getAuthorizedSystemsForRestApi()

    val jwkProvider = JwkProviderBuilder(configuration.getJwksUrl())
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
    }

    val systembrukerService = SystembrukerService(
        systembrukerGateway = SystembrukerGateway(
            httpClient = httpClient,
            clientId = configuration.getServiceAccountClientId(),
            clientSecret = configuration.getServiceAccountClientSecret(),
            scopes = configuration.getServiceAccountScopes(),
            tokenUrl = configuration.getTokenUrl()
        )
    )

    val systemCredentialsProvider = Oauth2ClientCredentialsProvider(
        monitoredHttpClient = MonitoredHttpClient(
            source = appId,
            destination = "nais-sts",
            httpClient = HttpClient(Apache) {
                install(JsonFeature) {
                    serializer = JacksonSerializer {
                        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    }
                }
                install (Logging) {
                    sl4jLogger("nais-sts")
                }
                engine {
                    customizeClient { setProxyRoutePlanner() }
                }
            }
        ),
        tokenUrl = configuration.getTokenUrl(),
        clientId = configuration.getServiceAccountClientId(),
        clientSecret = configuration.getServiceAccountClientSecret(),
        scopes = configuration.getServiceAccountScopes()
    )

    install(CallIdRequired)

    install(Routing) {
        authenticate {
            requiresCallId {
                prosesseringApis(
                    prosesseringV1Service = ProsesseringV1Service(
                        gosysService = GosysService(
                            joarkGateway = JoarkGateway(
                                httpClient = httpClient,
                                url = configuration.getOpprettJournalPostUrl(),
                                systembrukerService = systembrukerService
                            ),
                            oppgaveGateway = OppgaveGateway(
                                httpClient = httpClient,
                                url = configuration.getOpprettOppgaveUrl(),
                                systembrukerService = systembrukerService
                            )
                        ),
                        aktoerService = AktoerService(
                            aktoerGateway = AktoerGateway(
                                httpClient = httpClient,
                                systembrukerService = systembrukerService,
                                baseUrl = configuration.getAktoerRegisterBaseUrl()
                            )
                        ),
                        pdfV1Generator = PdfV1Generator(),
                        dokumentService = DokumentService(
                            dokumentGateway = DokumentGateway(
                                httpClient = httpClient,
                                systembrukerService = systembrukerService,
                                baseUrl = configuration.getPleiepengerDokumentBaseUrl()
                            )
                        )
                    )
                )
            }
        }
        DefaultProbeRoutes()
        MetricsRoute()
        HealthRoute(
            healthChecks = setOf(
                SystemCredentialsProviderHealthCheck(
                    systemCredentialsProvider = systemCredentialsProvider
                ),
                HttpRequestHealthCheck(
                    app = appId,
                    urlExpectedHttpStatusCodeMap = mapOf(
                        configuration.getJwksUrl() to HttpStatusCode.OK
                    )
                )
            )
        )
    }

    install(CallId) {
        fromXCorrelationIdHeader()
    }

    install(CallLogging) {
        correlationIdAndRequestIdInMdc()
        logRequests()
    }
}