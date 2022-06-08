package no.nav.helse.felles

import java.time.LocalDate

data class OpptjeningIUtlandet(
    val navn: String,
    val opptjeningType: OpptjeningType,
    val land: Land,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate
)

enum class OpptjeningType(val pdfTekst: String) {
    ARBEIDSTAKER("arbeidstaker"),
    FRILANSER("frilans")
}