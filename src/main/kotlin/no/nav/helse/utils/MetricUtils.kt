package no.nav.helse.utils

import no.nav.helse.felles.Barn
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

private val ZONE_ID = ZoneId.of("Europe/Oslo")

internal fun Double.erUnderEttAar() = 0.0 == this
internal fun Barn.idType(): String = when {
    fødselsnummer == null -> "ikke satt"
    fødselsnummer.erDnummer() -> "dnummer"
    else -> "fodselsnummer"
}

internal fun Barn.fødselsdato(): LocalDate? {
    if(fødselsnummer == null) return null

    val dag = if (fødselsnummer.erDnummer()) {
        val førsteSiffer = fødselsnummer.substring(0, 1).toInt().minus(4)
        "$førsteSiffer${fødselsnummer[1]}".toInt()
    } else {
        fødselsnummer.dagdel().toInt()
    }

    val måned = fødselsnummer.månedsdel().toInt()
    val år = fødselsnummer.årsdel().tosifretÅrTilFiresifretÅr().toInt()
    return LocalDate.of(år, måned, dag)
}


internal fun LocalDate.aarSiden(): Double {
    val alder = ChronoUnit.YEARS.between(this, LocalDate.now(ZONE_ID))
    if (alder in -18..-1) return 19.0
    return alder.absoluteValue.toDouble()
}

internal fun LocalDate.ukerSiden() = ChronoUnit.WEEKS.between(this, LocalDate.now(ZONE_ID)).absoluteValue.toString()
internal fun Boolean.tilJaEllerNei(): String = if (this) "Ja" else "Nei"
