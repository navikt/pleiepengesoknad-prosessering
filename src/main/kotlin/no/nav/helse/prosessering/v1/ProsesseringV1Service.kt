package no.nav.helse.prosessering.v1

import no.nav.helse.prosessering.Metadata

interface ProsesseringV1Service {
    suspend fun leggSoknadTilProsessering(
        melding: MeldingV1,
        metadata: Metadata
    )
}