package no.nav.helse.prosessering.v1

import java.time.Duration

internal val NormalArbeidsdag = Duration.ofHours(7).plusMinutes(30)
private val NormalArbeidsuke = Duration.ofHours(37).plusMinutes(30)

internal fun Duration.somTekst() : String {
    val timer = seconds / 3600
    val minutter = (seconds % 3600) / 60
    val timerTeskst = when (timer) {
        0L -> ""
        1L -> "$timer time"
        else -> "$timer timer"
    }
    val minutterTekst = when(minutter) {
        0L -> ""
        1L -> "$minutter minutt"
        else -> "$minutter minutter"
    }

    val mellomTekst = if (timerTeskst.isNotBlank() && minutterTekst.isNotBlank()) " og " else ""
    val avkortetTekst = if (this > NormalArbeidsdag) " (avkortet til 7 timer og 30 minutter)" else ""

    return "$timerTeskst$mellomTekst$minutterTekst$avkortetTekst"
}

internal fun TilsynsordningJa.prosentAvNormalArbeidsuke() : Double {
    val tilsyn = Duration.ZERO
        .plusOmIkkeNullOgAvkortTilNormalArbeidsdag(mandag)
        .plusOmIkkeNullOgAvkortTilNormalArbeidsdag(tirsdag)
        .plusOmIkkeNullOgAvkortTilNormalArbeidsdag(onsdag)
        .plusOmIkkeNullOgAvkortTilNormalArbeidsdag(torsdag)
        .plusOmIkkeNullOgAvkortTilNormalArbeidsdag(fredag)

    return if (tilsyn.isZero) return 0.0
    else (100.00 / NormalArbeidsuke.seconds) * tilsyn.seconds
}

private fun Duration.plusOmIkkeNullOgAvkortTilNormalArbeidsdag(duration: Duration?): Duration {
    return when {
        duration == null -> this
        duration > NormalArbeidsdag -> plus(NormalArbeidsdag)
        else -> plus(duration)
    }
}