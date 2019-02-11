package no.nav.helse.journalforing.gateway

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL

private val logger: Logger = LoggerFactory.getLogger("nav.JournalforingGateway")

/*
    https://dokmotinngaaende-q1.nais.preprod.local/rest/mottaInngaaendeForsendelse
 */
class JournalforingGateway(
    private val httpClient: HttpClient,
    private val joarkInngaaendeForsendelseUrl: URL
) {

    internal suspend fun jorunalfor(request: JournalPostRequest) : JournalPostResponse {
        return httpClient.post(joarkInngaaendeForsendelseUrl) {
            contentType(ContentType.Application.Json)
            body = request
        }
    }
}