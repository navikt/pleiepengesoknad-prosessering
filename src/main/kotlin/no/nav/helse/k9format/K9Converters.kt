package no.nav.helse.k9format

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

private object K9Converters {
    internal val MinutterPerTime = BigDecimal.valueOf(60)
}

internal fun Double.timerTilDuration() : Duration {
    val bigDecimal = BigDecimal.valueOf(this)
    val timer = bigDecimal.toLong()
    val minutter = bigDecimal
        .remainder(BigDecimal.ONE)
        .setScale(2, RoundingMode.HALF_UP)
        .times(K9Converters.MinutterPerTime)
        .toLong()

    return Duration
        .ofHours(timer)
        .plusMinutes(minutter)
}