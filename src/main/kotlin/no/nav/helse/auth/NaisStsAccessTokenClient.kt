package no.nav.helse.auth

import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.httpGet
import io.ktor.http.Url
import no.nav.helse.HttpError
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenResponse
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.*

private val logger: Logger = LoggerFactory.getLogger("nav.NaisStsAccessTokenClient")

internal class NaisStsAccessTokenClient(
    tokenEndpoint: URI,
    clientId: String,
    clientSecret: String
) : AccessTokenClient {

    private val authorizationHeader = getAuthorizationHeader(clientId, clientSecret)
    private val url = Url.buildURL(baseUrl = tokenEndpoint, queryParameters = mapOf(
        "grant_type" to listOf("client_credentials"),
        "scope" to listOf("openid")
    )).toString()

    override fun getAccessToken(scopes: Set<String>): AccessTokenResponse {
        val (request, response, result) = url.httpGet()
            .header(
                Headers.AUTHORIZATION to authorizationHeader
            ).responseString()

        return result.fold(
            { success ->
                val json = JSONObject(success)
                AccessTokenResponse(
                    accessToken = json.getString("access_token"),
                    tokenType = json.getString("token_type"),
                    expiresIn = json.getLong("expires_in")
                )
            },
            { error ->
                logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                logger.error(error.toString())
                throw HttpError(response.statusCode,"Feil ved henting av access token fra Nais STS.")
            }
        )
    }

    override fun getAccessToken(scopes: Set<String>, onBehalfOf: String): AccessTokenResponse {
        throw IllegalStateException("NaisSts støtter ikke onBehalfOf")
    }

    private fun getAuthorizationHeader(clientId: String, clientSecret: String) : String {
        val auth = "$clientId:$clientSecret"
        return "Basic ${Base64.getEncoder().encodeToString(auth.toByteArray())}"
    }
}