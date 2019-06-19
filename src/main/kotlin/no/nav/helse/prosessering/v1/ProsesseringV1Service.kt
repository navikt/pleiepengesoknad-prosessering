package no.nav.helse.prosessering.v1

interface ProsesseringV1Service {
    suspend fun leggSoknadTilProsessering(
        melding: MeldingV1,
        metadata: MetadataV1
    )
}