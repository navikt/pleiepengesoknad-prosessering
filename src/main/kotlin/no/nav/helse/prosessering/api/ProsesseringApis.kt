package no.nav.helse.prosessering.api

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import io.ktor.request.receive
import io.ktor.response.ApplicationResponse
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerService
import no.nav.helse.aktoer.Fodselsnummer
import no.nav.helse.dokument.DokumentService
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.v1.ProsesseringV1Service
import no.nav.helse.prosessering.v1.reportMetrics
import no.nav.helse.prosessering.v1.validate
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("nav.prosesseringApis")

fun Route.prosesseringApis(
    synkronProsesseringV1Service: ProsesseringV1Service,
    asynkronProsesseringV1Service: ProsesseringV1Service?,
    dokumentService: DokumentService,
    aktoerService: AktoerService
) {

    suspend fun medLagredeVedlegg(
        melding: MeldingV1,
        correlationId: CorrelationId) : MeldingV1 {
        return if (melding.vedlegg.isNotEmpty()) {
            logger.info("Lagrer ${melding.vedlegg.size} vedlegg.")
            val vedleggUrls = dokumentService.lagreVedlegg(
                vedlegg = melding.vedlegg,
                correlationId = correlationId,
                aktoerId = aktoerService.getAktorId(
                    fnr = Fodselsnummer(melding.soker.fodselsnummer),
                    correlationId = correlationId
                )
            )
            melding.medKunVedleggUrls(vedleggUrls)
        } else melding
    }

    post("v1/soknad") {
        val metadata = call.metadata()
        logger.info(metadata.toString())
        val melding = medLagredeVedlegg(call.melding(), CorrelationId(metadata.correlationId))
        if (call.request.prosesserAsynkront()) {
            call.prosesserMed(melding, metadata, asynkronProsesseringV1Service?:synkronProsesseringV1Service)
        } else  {
            call.prosesserMed(melding, metadata, synkronProsesseringV1Service)
        }
    }
}

private fun ApplicationCall.metadata() = Metadata(
    version = 1,
    correlationId = request.getCorrelationId(),
    requestId = response.getRequestId()
)

private suspend fun ApplicationCall.melding() : MeldingV1 {
    val melding = receive<MeldingV1>()
    melding.validate()
    return melding
}

private suspend fun ApplicationCall.prosesserMed(
    melding: MeldingV1,
    metadata: Metadata,
    prosesseringsV1Service: ProsesseringV1Service
) {
    val id = prosesseringsV1Service.leggSoknadTilProsessering(
        melding = melding,
        metadata = metadata
    )
    melding.reportMetrics()
    respond(HttpStatusCode.Accepted, mapOf("id" to id.id))
}

private fun ApplicationRequest.prosesserAsynkront() : Boolean {
    val queryParameterValue = queryParameters["async"]
    return queryParameterValue != null && queryParameterValue.equals("true", true)
}

private fun ApplicationRequest.getCorrelationId(): String {
    return header(HttpHeaders.XCorrelationId) ?: throw IllegalStateException("Correlation Id ikke satt")
}

private fun ApplicationResponse.getRequestId(): String? {
    return headers[HttpHeaders.XRequestId]
}