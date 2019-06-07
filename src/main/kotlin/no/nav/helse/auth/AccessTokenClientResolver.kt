package no.nav.helse.auth

import no.nav.helse.dusseldorf.ktor.auth.Client
import no.nav.helse.dusseldorf.ktor.auth.ClientSecretClient
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class AccessTokenClientResolver(
    clients : Map<String, Client>
) : HealthCheck {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger("nav.AccessTokenClientResolver")
        private const val NAIS_STS_ALIAS = "nais-sts"
    }

    private val naisStsClient = clients.getOrElse(NAIS_STS_ALIAS) {
        throw IllegalStateException("Client[$NAIS_STS_ALIAS] må være satt opp.")
    } as ClientSecretClient

    private val naisStsAccessTokenClient = NaisStsAccessTokenClient(
        clientId = naisStsClient.clientId(),
        clientSecret = naisStsClient.clientSecret,
        tokenEndpoint = naisStsClient.tokenEndpoint()
    )

    private val cachedNaisStsAccessTokenClient = CachedAccessTokenClient(naisStsAccessTokenClient)

    internal fun oppgaveAccessTokenClient() = cachedNaisStsAccessTokenClient
    internal fun dokumentAccessTokenClient() = cachedNaisStsAccessTokenClient
    internal fun joarkAccessTokenClient() = cachedNaisStsAccessTokenClient
    internal fun aktoerRegisterAccessTokenClient() = cachedNaisStsAccessTokenClient

    override suspend fun check(): Result {
        return try {
            naisStsAccessTokenClient.getAccessToken(setOf("openid"))
            Healthy("NaisStsAccessTokenClient", "Healthy!")
        } catch (cause: Throwable) {
            logger.error("Feil ved henting av access token fra Nais STS", cause)
            UnHealthy("NaisStsAccessTokenClient", "Unhealthy!")
        }
    }
}