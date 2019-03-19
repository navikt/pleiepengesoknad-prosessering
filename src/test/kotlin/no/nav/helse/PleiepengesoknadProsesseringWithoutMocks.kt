package no.nav.helse

import io.ktor.server.testing.withApplication

/**
 *  - Mer leslig loggformat
 *  - Setter proxy settings
 *  - Starter på annen port
 */
class PleiepengesoknadProsesseringWithoutMocks {
    companion object {

        @JvmStatic
        fun main(args: Array<String>) {

            System.setProperty("http.nonProxyHosts", "localhost")
            System.setProperty("http.proxyHost", "127.0.0.1")
            System.setProperty("http.proxyPort", "5001")
            System.setProperty("https.proxyHost", "127.0.0.1")
            System.setProperty("https.proxyPort", "5001")

            // nav.authorization.service_account.client_secret Må fortsatt settes som parameter ved oppstart utenom koden

            val pleiepengerJoarkLocalhost = false
            val pleiepengerSakLocalhost = false
            val pleiepengerOppgaveLocalhost = false
            val pleiepengerDokumentLocalhost = false

            val q1Args = TestConfiguration.asArray(TestConfiguration.asMap(
                port = 8093,
                tokenUrl = "https://security-token-service.nais.preprod.local/rest/v1/sts/token",
                jwkSetUrl = "https://security-token-service.nais.preprod.local/rest/v1/sts/jwks",
                issuer = "https://security-token-service.nais.preprod.local",
                aktoerRegisterBaseUrl = "https://app-q1.adeo.no/aktoerregister",
                opprettJournalPostUrl = "${if (pleiepengerJoarkLocalhost) "http://localhost:8113" else "https://pleiepenger-joark.nais.preprod.local"}/v1/journalforing",
                opprettSakUrl = "${if (pleiepengerSakLocalhost) "http://localhost:8103" else "https://pleiepenger-sak.nais.preprod.local"}/v1/sak",
                opprettOppgaveUrl = "${if (pleiepengerOppgaveLocalhost) "http://localhost:8123" else "https://pleiepenger-oppgave.nais.preprod.local"}/v1/oppgave",
                pleiepeingerDokumentBaseUrl = if (pleiepengerDokumentLocalhost) "http://localhost:8133" else "https://pleiepenger-dokument.nais.preprod.local",
                clientSecret = null
            ))

            withApplication { no.nav.helse.main(q1Args) }
        }
    }
}
