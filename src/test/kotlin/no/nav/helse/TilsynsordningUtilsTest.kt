package no.nav.helse

import no.nav.helse.felles.TilsynsordningJa
import no.nav.helse.prosessering.v1.NormalArbeidsdag
import no.nav.helse.prosessering.v1.prosentAvNormalArbeidsuke
import no.nav.helse.prosessering.v1.snittTilsynsTimerPerDag
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class TilsynsordningUtilsTest {
    @Test
    fun `Ingen dager tilsyn er 0%`() {
        val tilsyn = TilsynsordningJa(
            mandag = null,
            tirsdag = null,
            onsdag = null,
            torsdag = null,
            fredag = null
        )
        assertEquals(0.0, tilsyn.prosentAvNormalArbeidsuke())
    }

    @Test
    fun `7 timer og 30 minuttern hver dag er 100%`() {
        val tilsyn = TilsynsordningJa(
            mandag = NormalArbeidsdag,
            tirsdag = NormalArbeidsdag,
            onsdag = NormalArbeidsdag,
            torsdag = NormalArbeidsdag,
            fredag = NormalArbeidsdag
        )
        assertEquals(100.0, tilsyn.prosentAvNormalArbeidsuke())
    }

    @Test
    fun `Mer enn 37 timer og 30 minutter i løpet av en uke er 100%`() {
        val tilsyn = TilsynsordningJa(
            mandag = NormalArbeidsdag,
            tirsdag = NormalArbeidsdag.plusHours(1),
            onsdag = NormalArbeidsdag,
            torsdag = NormalArbeidsdag,
            fredag = NormalArbeidsdag.plusHours(1)
        )
        assertEquals(100.0, tilsyn.prosentAvNormalArbeidsuke())
    }

    @Test
    fun `1 og en halv time hver dag er 20%`() {
        val tilsyn = TilsynsordningJa(
            mandag = Duration.ofHours(1).plusMinutes(30),
            tirsdag = Duration.ofHours(1).plusMinutes(30),
            onsdag = Duration.ofHours(1).plusMinutes(30),
            torsdag = Duration.ofHours(1).plusMinutes(30),
            fredag = Duration.ofHours(1).plusMinutes(30)
        )
        assertEquals(20.0, tilsyn.prosentAvNormalArbeidsuke())
    }


    @Test
    fun `Tilsynsordning til k9Tilsynsordning`() {
        val tilsyn = TilsynsordningJa(
            mandag = Duration.parse("PT7H30M"),
            tirsdag = Duration.parse("PT4H30M"),
            onsdag = Duration.parse("PT2H30M"),
            torsdag = null,
            fredag = Duration.parse("PT5H")
        )

        assertEquals("PT3H54M", tilsyn.snittTilsynsTimerPerDag().toString())
    }
}
