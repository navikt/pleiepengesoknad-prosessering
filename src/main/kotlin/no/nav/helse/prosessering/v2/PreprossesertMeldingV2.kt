package no.nav.helse.prosessering.v2

import no.nav.k9.søknad.Søknad
import java.net.URI

data class PreprossesertMeldingV2(
    val dokumentUrls: List<List<URI>>,
    val søknad: Søknad,
    val interInfo: InternInfo
)

