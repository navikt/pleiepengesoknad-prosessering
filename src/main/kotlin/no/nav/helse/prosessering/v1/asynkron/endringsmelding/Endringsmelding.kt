package no.nav.helse.prosessering.v1.asynkron.endringsmelding

import no.nav.helse.felles.Søker
import no.nav.k9.søknad.Søknad
import java.net.URI

data class EndringsmeldingV1(
    val søker: Søker,
    val k9Format: Søknad,
)

data class PreprossesertEndringsmeldingV1(
    val søker: Søker,
    val k9FormatSøknad: Søknad,
    val dokumentUrls: List<List<URI>>
) {
    internal constructor(
        endringsmelding: EndringsmeldingV1,
        dokumentUrls: List<List<URI>>
    ) : this(
        søker = endringsmelding.søker,
        k9FormatSøknad = endringsmelding.k9Format,
        dokumentUrls = dokumentUrls
    )
}
