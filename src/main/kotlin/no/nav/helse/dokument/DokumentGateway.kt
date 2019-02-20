package no.nav.helse.dokument

import no.nav.helse.aktoer.AktoerId
import java.net.URL

/*
    TODO: Integrasjon mot "pleiepenger-dokument"
 */
class DokumentGateway{
    fun lagrePdf(
        pdf : ByteArray,
        aktoerId : AktoerId
    ) : URL {
        return URL("https://www.nav.no/dokument/123")
    }
}