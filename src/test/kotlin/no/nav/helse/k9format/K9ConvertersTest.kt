package no.nav.helse.k9format

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

internal class K9ConvertersTest {

    @Test
    internal fun `Desimaltimer til varighet`() {
        assertEquals(Duration.ofHours(37).plusMinutes(30), 37.5.timerTilDuration())
        assertEquals(Duration.ofMinutes(45), 0.75.timerTilDuration())
        assertEquals(Duration.ofMinutes(45), 0.745.timerTilDuration())
        assertEquals(Duration.ofMinutes(44), 0.744.timerTilDuration())
        assertEquals(Duration.ofHours(10).plusMinutes(48), 10.804999.timerTilDuration())
        assertEquals(Duration.ofHours(5), 5.0.timerTilDuration())
    }
}