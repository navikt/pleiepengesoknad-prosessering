package no.nav.helse

import no.nav.helse.prosessering.v1.Barn
import no.nav.helse.prosessering.v1.metricAlder
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BarnTest {
    private companion object {
        val now = LocalDate.now(ZoneId.of("Europe/Oslo"))
    }

    @Test
    fun `Test barnets alder`() {
        for (i in 0..18) {
            assertEquals(barn(i.toLong()).metricAlder()!!, i.toDouble())
        }

        for (i in 19..99) {
            assertTrue(barn(i.toLong()).metricAlder()!! > 18.00)
        }
    }

    private fun barn(forventetAlder : Long) : Barn {
        val fodselsdato = if (forventetAlder == 0L) now.minusDays(1) else now.minusYears(forventetAlder)
        val dag = fodselsdato.dayOfMonth.toString().padStart(2, '0')
        val maned = fodselsdato.monthValue.toString().padStart(2, '0')
        val ar = fodselsdato.year.toString().substring(2,4)
        val fodselsnummer = "$dag$maned${ar}12345"
        return Barn(
            fodselsnummer = fodselsnummer,
            alternativId = null,
            navn = null
        )
    }
}