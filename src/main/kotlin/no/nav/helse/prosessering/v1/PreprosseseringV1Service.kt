package no.nav.helse.prosessering.v1

import no.nav.helse.CorrelationId

internal class PreprosseseringV1Service {

    suspend internal fun preprosseser(
        melding: MeldingV1,
        correlationId: CorrelationId
    ) : UtgaendeMeldingV1 {
        return UtgaendeMeldingV1(a= "2")
    }
}