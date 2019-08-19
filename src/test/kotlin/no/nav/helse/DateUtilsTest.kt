package no.nav.helse

import no.nav.helse.prosessering.v1.DateUtils
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DateUtilsTest {
    @Test
    fun `Gir korrekt antall virkedager`() {
        var fraOgMed = LocalDate.of(2019, 8, 20)
        var tilOgMed = LocalDate.of(2019, 9, 18)

        assertEquals(22, DateUtils.antallVirkedager(fraOgMed, tilOgMed))

        // Over årsskifte
        fraOgMed = LocalDate.of(2019, 11, 6)
        tilOgMed = LocalDate.of(2020, 3, 3)

        assertEquals(85, DateUtils.antallVirkedager(fraOgMed, tilOgMed))

        // Lørdag til og med Søndag
        fraOgMed = LocalDate.of(2020, 2, 29)
        tilOgMed = LocalDate.of(2020, 3, 1)

        assertEquals(0, DateUtils.antallVirkedager(fraOgMed, tilOgMed))

        // En dag
        fraOgMed = LocalDate.of(2020, 3, 2)
        tilOgMed = LocalDate.of(2020, 3, 2)

        assertEquals(1, DateUtils.antallVirkedager(fraOgMed, tilOgMed))
    }
}