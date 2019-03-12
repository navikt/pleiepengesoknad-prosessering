package no.nav.helse.dokument

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.helse.CorrelationId
import no.nav.helse.ObjectMapper
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.Vedlegg
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL

private val logger: Logger = LoggerFactory.getLogger("nav.DokumentService")

class DokumentService(
    private val dokumentGateway: DokumentGateway
) {
    private val objectMapper = ObjectMapper.server()

    suspend fun lagreSoknadsOppsummeringPdf(
        pdf : ByteArray,
        aktoerId: AktoerId,
        correlationId: CorrelationId
    ) : URL {
        return dokumentGateway.lagreDokument(
            dokument = DokumentGateway.Dokument(
                content = pdf,
                contentType = "application/pdf",
                title = "Søknad om pleiepeinger"
            ),
            correlationId = correlationId,
            aktoerId = aktoerId
        )
    }

    suspend fun lagreSoknadsMelding(
        melding: MeldingV1,
        aktoerId: AktoerId,
        correlationId: CorrelationId
    ) : URL {
        return dokumentGateway.lagreDokument(
            dokument = DokumentGateway.Dokument(
                content = objectMapper.writeValueAsBytes(melding),
                contentType = "application/json",
                title = "Søknad om pleiepeinger som JSON"
            ),
            correlationId = correlationId,
            aktoerId = aktoerId
        )
    }

    suspend fun lagreVedlegg(
        vedlegg : List<Vedlegg>,
        aktoerId: AktoerId,
        correlationId : CorrelationId
    ) : List<URL> {
        logger.trace("Lagrer ${vedlegg.size} vedlegg")
        return coroutineScope {
            val futures = mutableListOf<Deferred<URL>>()
            vedlegg.forEach {
                futures.add(async {
                    dokumentGateway.lagreDokument(
                        dokument = DokumentGateway.Dokument(it),
                        correlationId = correlationId,
                        aktoerId = aktoerId
                    )
                })

            }
            futures.awaitAll()
        }
    }
}