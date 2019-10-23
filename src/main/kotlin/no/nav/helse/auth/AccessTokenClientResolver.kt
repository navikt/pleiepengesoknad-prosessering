package no.nav.helse.auth

import no.nav.helse.dusseldorf.ktor.auth.Client
import no.nav.helse.dusseldorf.ktor.auth.ClientSecretClient
import no.nav.helse.dusseldorf.ktor.auth.PrivateKeyClient
import no.nav.helse.dusseldorf.oauth2.client.FromCertificateHexThumbprint
import no.nav.helse.dusseldorf.oauth2.client.FromJwk
import no.nav.helse.dusseldorf.oauth2.client.SignedJwtAccessTokenClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class AccessTokenClientResolver(
    clients : Map<String, Client>
) {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(AccessTokenClientResolver::class.java)
        private const val NAIS_STS_ALIAS = "nais-sts"
        private const val AZURE_V2_ALIAS = "azure-v2"
    }

    private val naisStsClient = clients.getOrElse(NAIS_STS_ALIAS) {
        throw IllegalStateException("Client[$NAIS_STS_ALIAS] må være satt opp.")
    } as ClientSecretClient

    private val azureV2Client = clients.getOrElse(AZURE_V2_ALIAS) {
        throw IllegalStateException("Client[$AZURE_V2_ALIAS] må være satt opp.")
    } as PrivateKeyClient

    private val naisStsAccessTokenClient = NaisStsAccessTokenClient(
        clientId = naisStsClient.clientId(),
        clientSecret = naisStsClient.clientSecret,
        tokenEndpoint = naisStsClient.tokenEndpoint()
    )

    private val azureV2AccessTokenClient = SignedJwtAccessTokenClient(
        clientId = azureV2Client.clientId(),
        tokenEndpoint = azureV2Client.tokenEndpoint(),
        privateKeyProvider = FromJwk(azureV2Client.privateKeyJwk),
        keyIdProvider = FromCertificateHexThumbprint(azureV2Client.certificateHexThumbprint)
    )

    internal fun oppgaveAccessTokenClient() = azureV2AccessTokenClient
    internal fun dokumentAccessTokenClient() = azureV2AccessTokenClient
    internal fun joarkAccessTokenClient() = azureV2AccessTokenClient
    internal fun aktoerRegisterAccessTokenClient() = naisStsAccessTokenClient
    internal fun tpsProxyAccessTokenClient() = naisStsAccessTokenClient
}