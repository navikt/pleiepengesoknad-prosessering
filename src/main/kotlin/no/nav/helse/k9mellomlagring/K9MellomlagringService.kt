package no.nav.helse.k9mellomlagring

import no.nav.helse.CorrelationId
import java.net.URI

class K9MellomlagringService(
    val k9MellomlagringGateway: K9MellomlagringGateway
){
    internal suspend fun lagreDokument(
        dokument: Dokument,
        correlationId: CorrelationId
    ): URI {
        return k9MellomlagringGateway.lagreDokmenter(
            dokumenter = setOf(dokument),
            correlationId = correlationId
        ).first()
    }

    internal suspend fun slettDokumeter(
        urlBolks: List<List<URI>>,
        dokumentEier: DokumentEier,
        correlationId : CorrelationId
    ) {
        k9MellomlagringGateway.slettDokmenter(
            urls = urlBolks.flatten(),
            dokumentEier = dokumentEier,
            correlationId = correlationId
        )
    }
}