package no.nav.helse.barn

import no.nav.helse.aktoer.AktoerRegisterV1Gateway
import no.nav.helse.aktoer.AktørId
import no.nav.helse.tpsproxy.Attributt
import no.nav.helse.tpsproxy.Ident
import no.nav.helse.tpsproxy.TpsBarn
import no.nav.helse.tpsproxy.TpsProxyV1Gateway

internal class BarnOppslag(
    private val aktoerRegisterV1Gateway: AktoerRegisterV1Gateway,
    private val tpsProxyV1Gateway: TpsProxyV1Gateway
) {

    internal suspend fun barn(
        ident: Ident,
        attributter: Set<Attributt>
    ): Set<Barn>? {
        if (!attributter.etterspurtBarn()) return null

        val tpsBarn = tpsProxyV1Gateway.barn(
            ident = ident,
            attributter = attributter
        ) ?: return null

        return tpsBarn
            .filter { it.dødsdato == null }
            .map {
                Barn(
                    tpsBarn = it,
                    aktørId = aktoerRegisterV1Gateway.aktørId(
                        ident = ident,
                        attributter = attributter
                    )
                )
            }.toSet()
    }
}

internal fun Set<Attributt>.etterspurtBarn() =
    any { it.api.startsWith("barn[].") }

internal data class Barn(
    internal val tpsBarn: TpsBarn?,
    internal val aktørId: AktørId?
)