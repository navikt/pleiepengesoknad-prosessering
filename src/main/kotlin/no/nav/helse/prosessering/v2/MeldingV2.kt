package no.nav.helse.prosessering.v2

import no.nav.helse.aktoer.AktoerId
import no.nav.k9.søknad.Søknad
import java.net.URI

data class MeldingV2 (
    var vedleggUrls : List<URI> = listOf(),
    val søknad: Søknad,
    val interInfo: InternInfo
)

data class InternInfo(
    val internSøker: InternSøker

)
class InternSøker(
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
    val aktørId: AktoerId
)

