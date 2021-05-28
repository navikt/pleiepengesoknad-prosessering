package no.nav.helse.dokument

import no.nav.helse.CorrelationId
import no.nav.k9.søknad.Søknad
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

private val logger: Logger = LoggerFactory.getLogger("nav.DokumentService")

class DokumentService(
    private val dokumentGateway: DokumentGateway
) {
    private suspend fun lagreDokument(
        dokument: DokumentGateway.Dokument,
        aktørId: String,
        correlationId: CorrelationId
    ) : URI {
        return dokumentGateway.lagreDokmenter(
            dokumenter = setOf(dokument),
            correlationId = correlationId,
            aktørId = aktørId
        ).first()
    }

    internal suspend fun lagreSoknadsOppsummeringPdf(
        pdf : ByteArray,
        aktørId: String,
        correlationId: CorrelationId,
        dokumentbeskrivelse: String
    ) : URI {
        return lagreDokument(
            dokument = DokumentGateway.Dokument(
                content = pdf,
                contentType = "application/pdf",
                title = dokumentbeskrivelse
            ),
            aktørId = aktørId,
            correlationId = correlationId
        )
    }

    internal suspend fun lagreSoknadsMelding(
        k9FormatSøknad: Søknad,
        aktørId: String,
        correlationId: CorrelationId
    ) : URI {
        return lagreDokument(
            dokument = DokumentGateway.Dokument(
                content = JournalforingsFormat.somJson(k9FormatSøknad),
                contentType = "application/json",
                title = "Søknad om pleiepenger som JSON"
            ),
            aktørId = aktørId,
            correlationId = correlationId
        )
    }

    internal suspend fun slettDokumeter(
        urlBolks: List<List<URI>>,
        aktørId: String,
        correlationId : CorrelationId
    ) {
        val urls = mutableListOf<URI>()
        urlBolks.forEach { urls.addAll(it) }

        logger.trace("Sletter ${urls.size} dokumenter")
        dokumentGateway.slettDokmenter(
            urls = urls,
            aktoerId = aktørId,
            correlationId = correlationId
        )
    }
}

