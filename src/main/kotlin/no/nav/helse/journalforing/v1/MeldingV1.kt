package no.nav.helse.journalforing.v1

import java.time.ZonedDateTime

data class MeldingV1 (
    val aktoerId: String,
    val mottatt: ZonedDateTime,
    val sakId: String,
    val dokumenter: List<DokumentV1>,
    val tittel: String
)