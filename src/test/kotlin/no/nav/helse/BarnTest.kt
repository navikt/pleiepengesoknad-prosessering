package no.nav.helse

import io.prometheus.client.CollectorRegistry
import no.nav.helse.felles.Barn
import no.nav.helse.utils.aarSiden
import no.nav.helse.utils.erUnderEttAar
import no.nav.helse.utils.fødselsdato
import no.nav.helse.utils.ukerSiden
import org.junit.Before
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BarnTest {
    private companion object {
        val now = LocalDate.now(ZoneId.of("Europe/Oslo"))
    }

    @Before
    fun setUp() {
        CollectorRegistry.defaultRegistry.clear()
    }

    @Test
    fun `Test barnets alder i aar`() {
        for (i in 0..18) {
            assertEquals(barn(i.toLong()).fødselsdato()!!.aarSiden(), i.toDouble())
        }

        for (i in 19..99) {
            assertTrue(barn(i.toLong()).fødselsdato()!!.aarSiden() > 18.00)
        }
    }

    @Test
    fun `Barn under 1 aar i uker`() {
        for (i in 0..52) {
            val fodseldato = LocalDate.now().minusWeeks(i.toLong())
            assertTrue(fodseldato.aarSiden().erUnderEttAar())
            assertEquals("$i" , fodseldato.ukerSiden())
        }
        val fodseldato = LocalDate.now().minusWeeks(53)
        assertFalse(fodseldato.aarSiden().erUnderEttAar())
    }

    @Test
    fun `gitt at barnet er født 010193, forvent localdate med 1993-01-01`() {
        val fødselsdato = Barn(
            fødselsnummer = "01019312345",
            navn = "Ole Dole",
            aktørId = "11111111111"
        ).fødselsdato()

        assertEquals(LocalDate.parse("1993-01-01"), fødselsdato)
    }

    @Test
    fun `gitt at barnet er født 010101, forvent localdate med 2001-01-01`() {
        val fødselsdato = Barn(
            fødselsnummer = "01010112345",
            navn = "Ole Dole",
            aktørId = "11111111111"
        ).fødselsdato()

        assertEquals(LocalDate.parse("2001-01-01"), fødselsdato)
    }

    @Test
    fun `gitt at barnet er født 010110, forvent localdate med 2010-01-01`() {
        val fødselsdato = Barn(
            fødselsnummer = "01011012345",
            navn = "Ole Dole",
            aktørId = "11111111111"
        ).fødselsdato()

        assertEquals(LocalDate.parse("2010-01-01"), fødselsdato)
    }

    private fun barn(forventetAlder : Long) : Barn {
        val fodselsdato = if (forventetAlder == 0L) now.minusDays(1) else now.minusYears(forventetAlder)
        val dag = fodselsdato.dayOfMonth.toString().padStart(2, '0')
        val maned = fodselsdato.monthValue.toString().padStart(2, '0')
        val ar = fodselsdato.year.toString().substring(2,4)
        val fodselsnummer = "$dag$maned${ar}12345"
        return Barn(
            fødselsnummer = fodselsnummer,
            navn = "Ola Nordmann",
            aktørId = "11111111111"
        )
    }
}
