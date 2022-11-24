package no.nav.helse.felles

import java.time.Duration
import java.time.LocalDate

data class NormalArbeidstid (
    val timerPerUkeISnitt: Duration? = null,
    @Deprecated("Fjernes når nye endringer på arbeid er lansert.")
    val erLiktHverUke: Boolean? = null,
    @Deprecated("Fjernes når nye endringer på arbeid er lansert.")
    val timerFasteDager: PlanUkedager? = null
)

data class Arbeidsforhold(
    val normalarbeidstid: NormalArbeidstid,
    val arbeidIPeriode: ArbeidIPeriode
)

data class ArbeidIPeriode(
    val type: ArbeidIPeriodeType,
    val arbeiderIPerioden: ArbeiderIPeriodenSvar,
    val prosentAvNormalt: Double? = null,
    val timerPerUke: Duration? = null,
    val arbeidsuker: List<ArbeidsUke>? = null,
    @Deprecated("Fjernes når nye endringer på arbeid er lansert.")
    val erLiktHverUke: Boolean? = null,
    @Deprecated("Fjernes når nye endringer på arbeid er lansert.")
    val fasteDager: PlanUkedager? = null,
    @Deprecated("Fjernes når nye endringer på arbeid er lansert.")
    val enkeltdager: List<ArbeidstidEnkeltdag>? = null
)

@Deprecated("Fjernes når nye endringer på arbeid er lansert.")
data class ArbeidstidEnkeltdag(
    val dato: LocalDate,
    val arbeidstimer: Arbeidstimer
)

@Deprecated("Fjernes når nye endringer på arbeid er lansert.")
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

data class ArbeidsUke(
    val periode: Periode,
    val timer: Duration? = null
)

enum class ArbeidIPeriodeType {
    ARBEIDER_IKKE,
    ARBEIDER_VANLIG,
    ARBEIDER_PROSENT_AV_NORMALT,
    ARBEIDER_TIMER_I_SNITT_PER_UKE,
    ARBEIDER_ULIKE_UKER_TIMER,
    @Deprecated("Fjernes når nye endringer på arbeid er lansert.") ARBEIDER_ENKELTDAGER,
    @Deprecated("Fjernes når nye endringer på arbeid er lansert.") ARBEIDER_FASTE_UKEDAGER
}
