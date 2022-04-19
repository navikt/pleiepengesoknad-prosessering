package no.nav.helse.felles

import java.time.Duration
import java.time.LocalDate

data class NormalArbeidstid (
    val erLiktHverUke: Boolean? = null,
    val timerPerUkeISnitt: Double? = null,
    val timerFasteDager: PlanUkedager? = null
)

data class Arbeidsforhold(
    val normalarbeidstid: NormalArbeidstid,
    val arbeidIPeriode: ArbeidIPeriode
)

data class ArbeidIPeriode(
    val type: ArbeidIPeriodeType,
    val arbeiderIPerioden: ArbeiderIPeriodenSvar,
    val erLiktHverUke: Boolean? = null,
    val fasteDager: PlanUkedager? = null,
    val prosentAvNormalt: Double? = null,
    val timerPerUke: Duration? = null,
    val enkeltdager: List<ArbeidstidEnkeltdag>? = null
)

data class ArbeidstidEnkeltdag(
    val dato: LocalDate,
    val arbeidstimer: Arbeidstimer
)

data class Arbeidstimer(
    val normalTimer: Duration,
    val faktiskTimer: Duration
)

enum class ArbeiderIPeriodenSvar {
    SOM_VANLIG,
    REDUSERT,
    HELT_FRAVÆR;

    fun jobber() = this != HELT_FRAVÆR
}

enum class ArbeidIPeriodeType {
    ARBEIDER_IKKE,
    ARBEIDER_VANLIG,
    ARBEIDER_ENKELTDAGER,
    ARBEIDER_FASTE_UKEDAGER,
    ARBEIDER_PROSENT_AV_NORMALT,
    ARBEIDER_TIMER_I_SNITT_PER_UKE,
}