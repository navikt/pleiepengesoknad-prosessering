package no.nav.helse.prosessering.v1


import no.nav.helse.felles.Enkeltdag
import java.time.Duration

internal val NormalArbeidsdag = Duration.ofHours(7).plusMinutes(30)

internal fun Duration.somTekst(avkort: Boolean = true): String {
    if(this.isZero) return "0 timer og 0 minutter"

    val timer = seconds / 3600
    val minutter = (seconds % 3600) / 60
    val timerTeskst = when (timer) {
        0L -> ""
        1L -> "$timer time"
        else -> "$timer timer"
    }
    val minutterTekst = when (minutter) {
        0L -> ""
        1L -> "$minutter minutt"
        else -> "$minutter minutter"
    }

    val mellomTekst = if (timerTeskst.isNotBlank() && minutterTekst.isNotBlank()) " og " else ""
    val avkortetTekst = if (this > NormalArbeidsdag && avkort) " (avkortet til 7 timer og 30 minutter)" else ""

    return "$timerTeskst$mellomTekst$minutterTekst$avkortetTekst"
}

fun List<Enkeltdag>.sumTid(): Duration {
    var sumTid = Duration.ZERO
    forEach {
        sumTid = sumTid.plus(it.tid)
    }
    return sumTid
}