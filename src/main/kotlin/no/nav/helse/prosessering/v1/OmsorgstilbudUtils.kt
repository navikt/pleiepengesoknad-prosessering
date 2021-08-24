package no.nav.helse.prosessering.v1


import no.nav.helse.felles.Omsorgsdag
import java.time.Duration
import java.time.Month

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

fun Month.tilNorskMåned(): String {
    return when(this){
        Month.JANUARY -> "Januar"
        Month.FEBRUARY -> "Februar"
        Month.MARCH -> "Mars"
        Month.APRIL -> "April"
        Month.MAY -> "Mai"
        Month.JUNE -> "Juni"
        Month.JULY -> "Juli"
        Month.AUGUST -> "August"
        Month.SEPTEMBER -> "September"
        Month.OCTOBER -> "Oktober"
        Month.NOVEMBER -> "November"
        Month.DECEMBER -> "Desember"
    }
}

fun List<Omsorgsdag>.sumTid(): Duration {
    var sumTid = Duration.ZERO
    forEach {
        sumTid = sumTid.plus(it.tid)
    }
    return sumTid
}
