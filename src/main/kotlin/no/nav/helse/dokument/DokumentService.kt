package no.nav.helse.dokument

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.Vedlegg
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL

private val logger: Logger = LoggerFactory.getLogger("nav.DokumentService")

class DokumentService(
    private val dokumentGateway: DokumentGateway
) {
    private val objectMapper = jacksonObjectMapper().dusseldorfConfigured()


    private suspend fun lagreDokument(
        dokument: DokumentGateway.Dokument,
        aktoerId: AktoerId,
        correlationId: CorrelationId
    ) : URL {
        return dokumentGateway.lagreDokmenter(
            dokumenter = setOf(dokument),
            correlationId = correlationId,
            aktoerId = aktoerId
        ).first()
    }

    internal suspend fun lagreSoknadsOppsummeringPdf(
        pdf : ByteArray,
        aktoerId: AktoerId,
        correlationId: CorrelationId
    ) : URL {
        return lagreDokument(
            dokument = DokumentGateway.Dokument(
                content = pdf,
                contentType = "application/pdf",
                title = "Søknad om pleiepenger"
            ),
            aktoerId = aktoerId,
            correlationId = correlationId
        )
    }

    internal suspend fun lagreSoknadsMelding(
        melding: MeldingV1,
        aktoerId: AktoerId,
        correlationId: CorrelationId
    ) : URL {
        return lagreDokument(
            dokument = DokumentGateway.Dokument(
                content = melding.jsonUtenVedlegg(),
                contentType = "application/json",
                title = "Søknad om pleiepenger som JSON"
            ),
            aktoerId = aktoerId,
            correlationId = correlationId
        )
    }

    internal suspend fun lagreVedlegg(
        vedlegg : List<Vedlegg>,
        aktoerId: AktoerId,
        correlationId : CorrelationId
    ) : List<URL> {
        val dokumenter = vedlegg.map { DokumentGateway.Dokument(it) }.toSet()

        logger.trace("Lagrer ${vedlegg.size} vedlegg")
        return dokumentGateway.lagreDokmenter(
            dokumenter = dokumenter,
            aktoerId = aktoerId,
            correlationId = correlationId

        )
    }

    internal suspend fun slettDokumeter(
        urlBolks: List<List<URL>>,
        aktoerId: AktoerId,
        correlationId : CorrelationId
    ) {
        val urls = mutableListOf<URL>()
        urlBolks.forEach { urls.addAll(it) }

        logger.trace("Sletter ${urls.size} dokumenter")
        dokumentGateway.slettDokmenter(
            urls = urls,
            aktoerId = aktoerId,
            correlationId = correlationId
        )
    }


    private fun MeldingV1.jsonUtenVedlegg(): ByteArray {
        val node = objectMapper.valueToTree<ObjectNode>(this)
        node.remove("vedlegg")
        node.remove("vedlegg_urls")
        return objectMapper.writeValueAsBytes(node)
    }
}

