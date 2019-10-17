package no.nav.helse.barn

import no.nav.helse.CorrelationId
import no.nav.helse.tpsproxy.Ident
import no.nav.helse.tpsproxy.TpsNavn
import no.nav.helse.tpsproxy.TpsProxyV1Gateway

internal class BarnOppslag(
    private val tpsProxyV1Gateway: TpsProxyV1Gateway
) {

    internal suspend fun navn(ident: Ident, correlationId: CorrelationId): TpsNavn {
        return tpsProxyV1Gateway.getNavnforBarn(ident = ident, correlationId = correlationId)
    }
}