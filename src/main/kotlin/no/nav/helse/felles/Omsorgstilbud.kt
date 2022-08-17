package no.nav.helse.felles

import java.time.Duration
import java.time.LocalDate

data class Omsorgstilbud(
    val svar: OmsorgstilbudSvar? = null, //TODO 17/08/2022 - Fjerne nullable etter frontend er prodsatt
    val erLiktHverUke: Boolean? = null,
    val enkeltdager: List<Enkeltdag>? = null,
    val ukedager: PlanUkedager? = null
)

enum class OmsorgstilbudSvar {
    JA, NEI, USIKKER;

    internal fun somTekst() = when(this){
        JA -> "Ja"
        NEI -> "Nei"
        USIKKER -> "Usikker"
    }
}

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