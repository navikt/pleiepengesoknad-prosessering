package no.nav.helse.prosessering.v1

import java.time.DayOfWeek.*
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object DateUtils {

    internal fun antallVirkedager(fraOgMed: LocalDate, tilOgMed: LocalDate) : Long {
        val dager = ChronoUnit.DAYS.between(fraOgMed, tilOgMed) + 1
        val lordagerOgSondager = antallLordagerOgSondagerIPerioden(fraOgMed, tilOgMed)
        return dager - lordagerOgSondager
    }

    private fun antallLordagerOgSondagerIPerioden(fraOgMed: LocalDate, tilOgMed: LocalDate) : Long {
        var antallLordagerOgSondager = 0L
        var current = fraOgMed
        while (current.isBefore(tilOgMed) || current.isEqual(tilOgMed)) {
            if (current.erLordagEllerSondag()) antallLordagerOgSondager++
            current = current.plusDays(1)
        }
        return antallLordagerOgSondager
    }

    private fun LocalDate.erLordagEllerSondag() = dayOfWeek == SATURDAY || dayOfWeek == SUNDAY
}

internal fun ZonedDateTime.norskDag() = when(dayOfWeek) {
    MONDAY -> "Mandag"
    TUESDAY -> "Tirsdag"
    WEDNESDAY -> "Onsdag"
    THURSDAY -> "Torsdag"
    FRIDAY -> "Fredag"
    SATURDAY -> "Lørdag"
    else -> "Søndag"
}