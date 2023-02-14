package no.nav.helse

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.auth.AccessTokenClientResolver
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
import no.nav.helse.k9mellomlagring.K9MellomlagringGateway
import no.nav.helse.k9mellomlagring.K9MellomlagringService
import no.nav.helse.pdf.EndringsmeldingPDFGenerator
import no.nav.helse.pdf.SøknadPDFGenerator
import no.nav.helse.prosessering.v1.PreprosseseringV1Service
import no.nav.helse.prosessering.v1.asynkron.AsynkronProsesseringV1Service
import no.nav.helse.prosessering.v1.asynkron.endringsmelding.AsynkronEndringsmeldingProsesseringV1Service
import no.nav.helse.prosessering.v1.asynkron.endringsmelding.EndringsmeldingPreprosseseringV1Service
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

private val logger: Logger = LoggerFactory.getLogger("nav.PleiepengesoknadProsessering")

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


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

    val k9MellomlagringGateway = K9MellomlagringGateway(
        baseUrl = configuration.getK9MellomlagringBaseUrl(),
        accessTokenClient = accessTokenClientResolver.accessTokenClient(),
        k9MellomlagringScopes = configuration.getK9MellomlagringScopes(),
    )

    val k9MellomlagringService = K9MellomlagringService(k9MellomlagringGateway)

    val preprosseseringV1Service = PreprosseseringV1Service(
        k9MellomlagringService = k9MellomlagringService,
        søknadPDFGenerator = SøknadPDFGenerator()
    )

    val endringsmeldingPreprosseseringV1Service = EndringsmeldingPreprosseseringV1Service(
        endringsmeldingPDFGenerator = EndringsmeldingPDFGenerator(),
        k9MellomlagringService = k9MellomlagringService
    )

    val joarkGateway = JoarkGateway(
        baseUrl = configuration.getk9JoarkBaseUrl(),
        accessTokenClient = accessTokenClientResolver.accessTokenClient(),
        journalforeScopes = configuration.getJournalforeScopes()
    )

    val asynkronProsesseringV1Service = AsynkronProsesseringV1Service(
        kafkaConfig = configuration.getKafkaConfig(TYPE.SØKNAD),
        preprosseseringV1Service = preprosseseringV1Service,
        joarkGateway = joarkGateway,
        k9MellomlagringService = k9MellomlagringService
    )

    val asynkronEndringsmeldingProsesseringV1Service = AsynkronEndringsmeldingProsesseringV1Service(
        kafkaConfig = configuration.getKafkaConfig(TYPE.ENDRINGSMELDING),
        endringsmeldingPreprosseseringV1Service = endringsmeldingPreprosseseringV1Service,
        joarkGateway = joarkGateway,
        k9MellomlagringService = k9MellomlagringService
    )


    environment.monitor.subscribe(ApplicationStopping) {
        logger.info("Stopper AsynkronProsesseringV1Service.")
        asynkronProsesseringV1Service.stop()
        logger.info("AsynkronProsesseringV1Service Stoppet.")

        /*logger.info("Stopper asynkronEndringsmeldingProsesseringV1Service.")
        asynkronEndringsmeldingProsesseringV1Service.stop()
        logger.info("asynkronEndringsmeldingProsesseringV1Service Stoppet.")*/

        CollectorRegistry.defaultRegistry.clear()
    }

    install(Routing) {
        MetricsRoute()
        HealthRoute(
            path = Paths.DEFAULT_ALIVE_PATH,
            healthService = HealthService(
                healthChecks = asynkronProsesseringV1Service.isReadyChecks().toSet()
                    /*.plus(asynkronEndringsmeldingProsesseringV1Service.isReadyChecks().toSet())*/
            )
        )
        get(Paths.DEFAULT_READY_PATH) {
            call.respondText("READY")
        }
        HealthRoute(
            healthService = HealthService(
                healthChecks = mutableSetOf(
                    k9MellomlagringGateway,
                    joarkGateway,
                    HttpRequestHealthCheck(
                        mapOf(
                            Url.healthURL(configuration.getK9MellomlagringBaseUrl()) to HttpRequestHealthConfig(
                                expectedStatus = HttpStatusCode.OK
                            ),
                            Url.healthURL(configuration.getk9JoarkBaseUrl()) to HttpRequestHealthConfig(
                                expectedStatus = HttpStatusCode.OK
                            )
                        )
                    )
                )
                    .plus(asynkronProsesseringV1Service.healthChecks()).toSet()
                    /*.plus(asynkronEndringsmeldingProsesseringV1Service.healthChecks()).toSet()*/
            )
        )
    }
}

private fun Url.Companion.healthURL(baseUrl: URI) = Url.buildURL(baseUrl = baseUrl, pathParts = listOf("health"))

internal fun ObjectMapper.pleiepengerKonfiguert(): ObjectMapper = dusseldorfConfigured()
    .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
    .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
