package no.nav.helse.prosessering.v1.asynkron.endringsmelding

import no.nav.helse.felles.Søker
import no.nav.k9.søknad.Søknad
import java.net.URI

data class EndringsmeldingV1(
    val søker: Søker,
    val harBekreftetOpplysninger: Boolean,
    val harForståttRettigheterOgPlikter: Boolean,
    val k9Format: String
)

data class PreprossesertEndringsmeldingV1(
    val søker: Søker,
    val k9FormatSøknad: Søknad,
    val dokumentUrls: List<List<URI>>
) {
    internal constructor(
        endringsmelding: EndringsmeldingV1,
        dokumentUrls: List<List<URI>>,
        k9Format: Søknad
    ) : this(
        søker = endringsmelding.søker,
        k9FormatSøknad = k9Format,
        dokumentUrls = dokumentUrls
    )
}
