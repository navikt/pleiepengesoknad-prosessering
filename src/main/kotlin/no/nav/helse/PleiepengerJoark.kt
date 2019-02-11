package no.nav.helse

import io.ktor.application.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.*
import io.ktor.jackson.jackson
import io.ktor.routing.Routing
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.journalforing.api.journalforingApis
import no.nav.helse.journalforing.api.metadataStatusPages
import no.nav.helse.journalforing.gateway.JournalforingGateway
import no.nav.helse.journalforing.v1.JournalforingV1Service
import no.nav.helse.validering.valideringStatusPages
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("nav.PleiepengerJoark")

fun main(args: Array<String>): Unit  = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
fun Application.pleiepengerJoark() {
    val joarkHttpClient = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = JacksonSerializer{
                ObjectMapper.joark(this)
            }
        }
    }
    val configuration = Configuration(environment.config)
    val authorizedSystems = configuration.getAuthorizedSystemsForRestApi() // TODO: Check JWT claims to ensure proper system.

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

    install(Routing) {
        monitoring()
        journalforingApis(
            journalforingV1Service = JournalforingV1Service(
                journalforingGateway = JournalforingGateway(
                    httpClient = joarkHttpClient,
                    joarkInngaaendeForsendelseUrl = configuration.getJoarkInngaaendeForseldenseUrl()
                )
            )
        )
    }

    install(CallId) {
        header("Nav-Call-Id")
    }

    install(CallLogging) {
        callIdMdc("call_id")
    }
}