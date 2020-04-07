package no.nav.helse.dokument

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.ettersending.Ettersending
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

private val logger: Logger = LoggerFactory.getLogger("nav.DokumentService")

class DokumentService(
    private val dokumentGateway: DokumentGateway
) {
    private suspend fun lagreDokument(
        dokument: DokumentGateway.Dokument,
        aktoerId: AktoerId,
        correlationId: CorrelationId
    ) : URI {
        return dokumentGateway.lagreDokmenter(
            dokumenter = setOf(dokument),
            correlationId = correlationId,
            aktoerId = aktoerId
        ).first()
    }

    internal suspend fun lagreSoknadsOppsummeringPdf(
        pdf : ByteArray,
        aktoerId: AktoerId,
        correlationId: CorrelationId,
        dokumentbeskrivelse: String
    ) : URI {
        return lagreDokument(
            dokument = DokumentGateway.Dokument(
                content = pdf,
                contentType = "application/pdf",
                title = dokumentbeskrivelse
            ),
            aktoerId = aktoerId,
            correlationId = correlationId
        )
    }

    internal suspend fun lagreSoknadsMelding(
        melding: MeldingV1,
        aktoerId: AktoerId,
        correlationId: CorrelationId
    ) : URI {
        return lagreDokument(
            dokument = DokumentGateway.Dokument(
                content = JournalforingsFormat.somJson(melding),
                contentType = "application/json",
                title = "Søknad om pleiepenger som JSON"
            ),
            aktoerId = aktoerId,
            correlationId = correlationId
        )
    }

    internal suspend fun lagreEttersendingMelding(
        melding: Ettersending,
        aktørId: AktoerId,
        correlationId: CorrelationId
    ) : URI {
        return lagreDokument(
            dokument = DokumentGateway.Dokument(
                content = JournalforingsFormat.somJsonEttersending(melding),
                contentType = "application/json",
                title = "Søknad om ettersending som JSON"
            ),
            aktoerId = aktørId,
            correlationId = correlationId
        )
    }

    internal suspend fun slettDokumeter(
        urlBolks: List<List<URI>>,
        aktørId: AktoerId,
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

