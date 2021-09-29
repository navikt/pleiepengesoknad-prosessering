package no.nav.helse.utils

import java.time.DayOfWeek
import java.time.DayOfWeek.*
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.streams.toList

object DateUtils {

    internal fun antallVirkedager(fraOgMed: LocalDate, tilOgMed: LocalDate): Long =
        fraOgMed.datesUntil(tilOgMed.plusDays(1)).toList()
            .filterNot { it.dayOfWeek == SATURDAY || it.dayOfWeek == SUNDAY }
            .size.toLong()
}

internal fun ZonedDateTime.somNorskDag() = dayOfWeek.somNorskDag()

internal fun DayOfWeek.somNorskDag() = when(this) {
    MONDAY -> "Mandag"
    TUESDAY -> "Tirsdag"
    WEDNESDAY -> "Onsdag"
    THURSDAY -> "Torsdag"
    FRIDAY -> "Fredag"
    SATURDAY -> "Lørdag"
    else -> "Søndag"
}