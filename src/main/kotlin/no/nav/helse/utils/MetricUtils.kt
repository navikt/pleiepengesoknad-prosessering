package no.nav.helse.utils

import no.nav.helse.felles.Barn
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

private val ZONE_ID = ZoneId.of("Europe/Oslo")

internal fun Double.erUnderEttAar() = 0.0 == this
internal fun Barn.idType(): String = "fodselsnummer"

internal fun Barn.fødselsdato(): LocalDate {
    val dag = fødselsnummer.substring(0, 2).toInt()
    val maned = fødselsnummer.substring(2, 4).toInt()
    val ar = "20${fødselsnummer.substring(4, 6)}".toInt()
    return LocalDate.of(ar, maned, dag)
}

internal fun LocalDate.aarSiden(): Double {
    val alder = ChronoUnit.YEARS.between(this, LocalDate.now(ZONE_ID))
    if (alder in -18..-1) return 19.0
    return alder.absoluteValue.toDouble()
}

internal fun LocalDate.ukerSiden() = ChronoUnit.WEEKS.between(this, LocalDate.now(ZONE_ID)).absoluteValue.toString()
internal fun Boolean.tilJaEllerNei(): String = if (this) "Ja" else "Nei"
