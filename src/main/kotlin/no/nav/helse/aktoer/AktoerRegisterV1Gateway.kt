package no.nav.helse.aktoer

import no.nav.helse.tpsproxy.Attributt
import no.nav.helse.tpsproxy.Ident

internal class AktoerRegisterV1Gateway(
    private val aktørRegisterV1: AktoerregisterV1
) {
    internal companion object {
        private val støttedeAttributter = setOf(
            Attributt.aktørId,
            Attributt.barnAktørId
        )
    }

    internal suspend fun aktørId(
        ident: Ident
    ): AktørId? {
        return aktørRegisterV1.aktørId(ident)
    }
}