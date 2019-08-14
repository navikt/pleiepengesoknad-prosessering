package no.nav.helse.prosessering.v1

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

internal object ArbeidsgiverUtils {
    private const val HUNDRE = 100.00

    private fun kalkulerProsentAndelAvNormalArbeidsuke(
        normalArbeidsuke: Duration,
        redusertArbeidsuke: Duration) = BigDecimal(HUNDRE.div(normalArbeidsuke.seconds).times(redusertArbeidsuke.seconds)).setScale(2, RoundingMode.HALF_UP).toDouble()

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

    internal fun prosentAvNormalArbeidsuke(arbeidsgivere: Arbeidsgivere) : Double {
        var normalArbeidsuke = Duration.ZERO
        var redusertArbeidsuke = Duration.ZERO

        arbeidsgivere.organisasjoner
            .filter { it.normalArbeidsuke != null && it.redusertArbeidsuke != null }
            .forEach {
            normalArbeidsuke = normalArbeidsuke.plus(it.normalArbeidsuke)
            redusertArbeidsuke = redusertArbeidsuke.plus(it.redusertArbeidsuke)
        }

        return if (normalArbeidsuke.isZero) 0.0
        else kalkulerProsentAndelAvNormalArbeidsuke(normalArbeidsuke, redusertArbeidsuke)
    }
}
internal fun Double.formatertMedToDesimaler() = String.format("%.2f", this)