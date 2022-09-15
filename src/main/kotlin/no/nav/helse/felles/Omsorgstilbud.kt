package no.nav.helse.felles

import java.time.Duration
import java.time.LocalDate

data class Omsorgstilbud(
    val svarFortid: OmsorgstilbudSvarFortid? = null,
    val svarFremtid: OmsorgstilbudSvarFremtid? = null,
    val erLiktHverUke: Boolean? = null,
    val enkeltdager: List<Enkeltdag>? = null,
    val ukedager: PlanUkedager? = null
)

enum class OmsorgstilbudSvarFortid(val pdfTekst: String) { JA("Ja"), NEI("Nei")}
enum class OmsorgstilbudSvarFremtid(val pdfTekst: String) { JA("Ja"), NEI("Nei"), USIKKER("Usikker") }

data class Enkeltdag(
    val dato: LocalDate,
    val tid: Duration
)

data class PlanUkedager(
    val mandag: Duration? = null,
    val tirsdag: Duration? = null,
    val onsdag: Duration? = null,
    val torsdag: Duration? = null,
    val fredag: Duration? = null
)