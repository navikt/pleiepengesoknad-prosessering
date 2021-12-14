package no.nav.helse.auth

import no.nav.helse.dusseldorf.ktor.auth.Client
import no.nav.helse.dusseldorf.ktor.auth.PrivateKeyClient
import no.nav.helse.dusseldorf.oauth2.client.DirectKeyId
import no.nav.helse.dusseldorf.oauth2.client.FromJwk
import no.nav.helse.dusseldorf.oauth2.client.SignedJwtAccessTokenClient

internal class AccessTokenClientResolver(
    clients : Map<String, Client>
) {
    private val AZURE_V2_ALIAS = "azure-v2"

    private val azureV2Client = clients.getOrElse(AZURE_V2_ALIAS) {
        throw IllegalStateException("Client[$AZURE_V2_ALIAS] må være satt opp.")
    } as PrivateKeyClient

    private val azureV2AccessTokenClient = SignedJwtAccessTokenClient(
        clientId = azureV2Client.clientId(),
        tokenEndpoint = azureV2Client.tokenEndpoint(),
        privateKeyProvider = FromJwk(azureV2Client.privateKeyJwk),
        keyIdProvider = DirectKeyId(azureV2Client.certificateHexThumbprint)
    )

    internal fun accessTokenClient() = azureV2AccessTokenClient
}
