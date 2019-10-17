package no.nav.helse.tpsproxy

import no.nav.helse.CorrelationId

internal class TpsProxyV1Gateway(
    private val tpsProxyV1: TpsProxyV1
) {

    internal suspend fun getNavnforBarn(ident: Ident, correlationId: CorrelationId): TpsNavn {
       return tpsProxyV1.navn(ident = ident, correlationId = correlationId)
    }
}