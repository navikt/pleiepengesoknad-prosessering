package no.nav.helse.utils

import java.time.LocalDate

fun String.dagdel() = substring(0, 2)
fun String.månedsdel() = substring(2, 4)
fun String.årsdel() = substring(4, 6)

fun String.firesifretÅrTilTosifretÅr() = substring(2, 4)

fun String.tosifretÅrTilFiresifretÅr(): String =
    if (toInt() <= LocalDate.now().year.toString().firesifretÅrTilTosifretÅr().toInt()) "20$this"
    else "19$this"

fun String.erDnummer(): Boolean = this.substring(0, 1).toInt() >= 4
