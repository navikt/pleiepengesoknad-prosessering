package no.nav.helse.auth

import no.nav.helse.dusseldorf.ktor.auth.Client
import no.nav.helse.dusseldorf.ktor.auth.ClientSecretClient
import no.nav.helse.dusseldorf.oauth2.client.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class AccessTokenClientResolver(
    clients : Map<String, Client>
) {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(AccessTokenClientResolver::class.java)
        private const val AZURE_V2_ALIAS = "azure-v2"
    }

    private val azureV2Client = clients.getOrElse(AZURE_V2_ALIAS) {
        throw IllegalStateException("Client[$AZURE_V2_ALIAS] må være satt opp.")
    } as ClientSecretClient

    private val azureV2AccessTokenClient = ClientSecretAccessTokenClient(
        clientId = azureV2Client.clientId(),
        tokenEndpoint = azureV2Client.tokenEndpoint(),
        clientSecret = azureV2Client.clientSecret,
    )

    internal fun dokumentAccessTokenClient() = azureV2AccessTokenClient
    internal fun joarkAccessTokenClient() = azureV2AccessTokenClient
}