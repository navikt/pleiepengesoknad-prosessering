package no.nav.helse.aktoer

import no.nav.helse.CorrelationId

class AktoerService(
    private val aktoerGateway: AktoerGateway
){
    suspend fun getAktorId(
        fnr: Fodselsnummer,
        correlationId: CorrelationId
    ): AktoerId {
        return aktoerGateway.getAktoerId(fnr, correlationId)
    }
}