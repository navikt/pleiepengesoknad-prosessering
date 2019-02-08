package no.nav.helse.journalforing.gateway

import io.ktor.client.HttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("nav.JournalforingGateway")

class JournalforingGateway(
    private val httpClient: HttpClient
) {

    internal suspend fun jorunalfor(request: JournalPostRequest) : JournalPostResponse {
        return JournalPostResponse(
            journalpostId = "123",
            journalTilstand = JournalTilstand.ENDELIG_JOURNALFOERT.name,
            dokumentIdListe = listOf()
        )
    }
}