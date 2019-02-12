package no.nav.helse.sak.v1

import no.nav.helse.sak.FagSystem
import no.nav.helse.sak.AktoerId
import no.nav.helse.sak.Tema
import no.nav.helse.sak.gateway.OpprettSakRequest

object OpprettSakRequestV1Factory {
    fun instance(
        fagSystem: FagSystem,
        tema: Tema,
        aktoerId: AktoerId
    ) : OpprettSakRequest {
        return OpprettSakRequest(
            applikasjon = fagSystem.kode,
            tema = tema.value,
            aktoerId = aktoerId.value
        )
    }
}