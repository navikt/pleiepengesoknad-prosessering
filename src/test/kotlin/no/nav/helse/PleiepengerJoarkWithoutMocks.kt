package no.nav.helse

import io.ktor.server.testing.withApplication

/**
 *  - Mer leslig loggformat
 *  - Setter proxy settings
 *  - Starter på annen port
 */
class PleiepengerJoarkWithoutMocks {
    companion object {

        @JvmStatic
        fun main(args: Array<String>) {

            System.setProperty("http.nonProxyHosts", "localhost")
            System.setProperty("http.proxyHost", "127.0.0.1")
            System.setProperty("http.proxyPort", "5001")
            System.setProperty("https.proxyHost", "127.0.0.1")
            System.setProperty("https.proxyPort", "5001")

            // nav.authorization.service_account.password Må fortsatt settes som parameter ved oppstart utenom koden
            val testArgs = arrayOf(
                "-P:ktor.deployment.port=8888",
                "-P:nav.authorization.token_url=https://security-token-service.nais.preprod.local/rest/v1/sts/token",
                "-P:nav.joark.inngaaende_forsendelse_url=https://dokmotinngaaende-q1.nais.preprod.local/rest/mottaInngaaendeForsendelse"
            )

            withApplication { no.nav.helse.main(testArgs) }
        }
    }
}
