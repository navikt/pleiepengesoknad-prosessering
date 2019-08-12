package no.nav.helse.prosessering.v1

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

internal object ArbeidsgiverUtils {
    private const val HUNDRE = 100.00

    private fun kalkulerProsentAndelAvNormalArbeidsuke(
        normalArbeidsuke: Duration,
        redusertArbeidsuke: Duration) = BigDecimal(HUNDRE.div(normalArbeidsuke.seconds).times(redusertArbeidsuke.seconds)).setScale(2, RoundingMode.HALF_UP).toDouble()
    private fun Double.formatertMedToDesimaler() = String.format("%.2f", this)

    private fun Long.formaterMinutter() = "$this minutt${ if(this > 1) "er" else ""}"
    private fun Long.formaterTimer() = "$this time${ if(this > 1) "r" else ""}"

    private fun Duration.formaterTilTimerOgMinutter() : String {
        val timer = seconds / 3600
        val minutter = (seconds % 3600) / 60
        return if (timer > 0 && minutter > 0) "${timer.formaterTimer()} og ${minutter.formaterMinutter()}"
        else if (timer > 0) timer.formaterTimer()
        else minutter.formaterMinutter()
    }

    internal fun formaterArbeidsuker(
        normalArbeidsuke: Duration?,
        redusertArbeidsuke: Duration?
    ) : String? {
        return if (normalArbeidsuke == null || redusertArbeidsuke == null) null
        else "Kan jobbe ${kalkulerProsentAndelAvNormalArbeidsuke(normalArbeidsuke, redusertArbeidsuke).formatertMedToDesimaler()}% av en normal arbeidsuke på ${normalArbeidsuke.formaterTilTimerOgMinutter()}."
    }
}