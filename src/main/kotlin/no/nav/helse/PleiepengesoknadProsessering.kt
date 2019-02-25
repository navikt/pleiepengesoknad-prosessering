package no.nav.helse

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.*
import io.ktor.http.HttpHeaders
import io.ktor.jackson.jackson
import io.ktor.request.header
import io.ktor.response.header
import io.ktor.routing.Routing
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.aktoer.AktoerGateway
import no.nav.helse.aktoer.AktoerService
import no.nav.helse.dokument.DokumentGateway
import no.nav.helse.gosys.GosysService
import no.nav.helse.gosys.JoarkGateway
import no.nav.helse.gosys.OppgaveGateway
import no.nav.helse.gosys.SakGateway
import no.nav.helse.prosessering.api.metadataStatusPages
import no.nav.helse.prosessering.api.prosesseringApis
import no.nav.helse.prosessering.v1.PdfV1Generator
import no.nav.helse.prosessering.v1.ProsesseringV1Service
import no.nav.helse.systembruker.SystembrukerGateway
import no.nav.helse.systembruker.SystembrukerService
import no.nav.helse.validering.valideringStatusPages
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.ProxySelector
import java.util.*
import java.util.concurrent.TimeUnit

private val logger: Logger = LoggerFactory.getLogger("nav.PleiepengesoknadProsessering")
private const val GENERATED_REQUEST_ID_PREFIX = "generated-"


fun main(args: Array<String>): Unit  = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
fun Application.pleiepengesoknadProsessering() {
    val collectorRegistry = CollectorRegistry.defaultRegistry
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
    configuration.logIndirectlyUsedConfiguration()

    val authorizedSystems = configuration.getAuthorizedSystemsForRestApi()

    val jwkProvider = JwkProviderBuilder(configuration.getJwksUrl())
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    install(Authentication) {
        jwt {
            verifier(jwkProvider, configuration.getIssuer())
            realm = "pleiepengesoknad-prosessering"
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
            ObjectMapper.server(this)
        }
    }

    install(StatusPages) {
        defaultStatusPages()
        valideringStatusPages()
        metadataStatusPages()
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

    install(Routing) {
        authenticate {

        }
        // TODO: Legg til under authenticate når vi får testet gjennom
        prosesseringApis(
            prosesseringV1Service = ProsesseringV1Service(
                gosysService = GosysService(
                    joarkGateway = JoarkGateway(
                        httpClient = httpClient,
                        url = configuration.getOpprettJournalPostUrl(),
                        systembrukerService = systembrukerService
                    ),
                    sakGateway = SakGateway(
                        httpClient = httpClient,
                        url = configuration.getOpprettSakurl(),
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
                dokumentGateway = DokumentGateway(
                    httpClient = httpClient,
                    systembrukerService = systembrukerService,
                    baseUrl = configuration.getPleiepengerDokumentBaseUrl()
                )
            )
        )
        monitoring(
            collectorRegistry = collectorRegistry
        )
    }

    install(CallId) {
        header(HttpHeaders.XCorrelationId)
    }

    install(CallLogging) {
        callIdMdc("correlation_id")
        mdc("request_id") { call ->
            val requestId = call.request.header(HttpHeaders.XRequestId)?.removePrefix(GENERATED_REQUEST_ID_PREFIX) ?: "$GENERATED_REQUEST_ID_PREFIX${UUID.randomUUID()}"
            call.response.header(HttpHeaders.XRequestId, requestId)
            requestId
        }
    }
}

private fun HttpAsyncClientBuilder.setProxyRoutePlanner() {
    setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
}