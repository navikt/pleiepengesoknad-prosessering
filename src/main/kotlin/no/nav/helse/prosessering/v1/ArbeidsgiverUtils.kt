package no.nav.helse.prosessering.v1

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

internal object ArbeidsgiverUtils {
    private val ETT_MINUTT = Duration.ofMinutes(1)
    private const val HUNDRE = 100.00

    private fun kalkulerProsentAndelAvNormalArbeidsuke(
        normalArbeidsuke: Duration,
        redusertArbeidsuke: Duration) = BigDecimal(HUNDRE.div(normalArbeidsuke.seconds).times(redusertArbeidsuke.seconds)).setScale(2, RoundingMode.HALF_UP).toDouble()

    private fun Duration.erSatt() = compareTo(ETT_MINUTT) >= 0

    private fun Long.formaterMinutter() = "$this minutt${ if(this > 1) "er" else ""}"
    private fun Long.formaterTimer() = "$this time${ if(this > 1) "r" else ""}"
    private fun Duration.formaterTilTimerOgMinutter() : String {
        val timer = seconds / 3600
        val minutter = (seconds % 3600) / 60
        return if (timer > 0 && minutter > 0) "${timer.formaterTimer()} og ${minutter.formaterMinutter()}"
        else if (timer > 0) timer.formaterTimer()
        else minutter.formaterMinutter()
    }

    internal fun prosentAvNormalArbeidsuke(
        normalArbeidsuke: Duration?,
        redusertArbeidsuke: Duration?
    ) : Double? {
        return if (normalArbeidsuke == null || redusertArbeidsuke == null) null
        else if (redusertArbeidsuke.isZero) 0.0
        else kalkulerProsentAndelAvNormalArbeidsuke(normalArbeidsuke, redusertArbeidsuke)
    }

    internal fun totalNormalArbeidsuke(arbeidsgivere: Arbeidsgivere) : String? {
        var normalArbeidsuke = Duration.ZERO

        arbeidsgivere.organisasjoner
            .filter { it.normalArbeidsuke != null }
            .forEach { normalArbeidsuke = normalArbeidsuke.plus(it.normalArbeidsuke) }

        return when {
            normalArbeidsuke.erSatt() -> "Jobber normalt ${normalArbeidsuke.formaterTilTimerOgMinutter()} per uke."
            else -> null
        }
    }
}
internal fun Double.formatertMedToDesimaler() = String.format("%.2f", this)
internal fun Organisasjon.formaterOrganisasjonsnummer() = if (organisasjonsnummer.length == 9) "${organisasjonsnummer.substring(0,3)} ${organisasjonsnummer.substring(3,6)} ${organisasjonsnummer.substring(6)}" else organisasjonsnummer