package no.nav.helse

import no.nav.helse.felles.Omsorgsdag
import no.nav.helse.felles.Omsorgstilbud
import no.nav.helse.felles.VetOmsorgstilbud
import no.nav.helse.prosessering.v1.beregnTidPerMåned
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import kotlin.test.assertEquals

class OmsorgstilbudTest {

    @Test
    fun `Omsorgstilbud enkeltdager blir til riktig totaltid per måned`(){
        val omsorgstilbud = Omsorgstilbud(
            vetOmsorgstilbud = VetOmsorgstilbud.VET_ALLE_TIMER,
            enkeltDager = listOf(
                Omsorgsdag(dato = LocalDate.parse("2020-01-01"), tid = Duration.ofHours(10)),
                Omsorgsdag(dato = LocalDate.parse("2020-01-03"), tid = Duration.ofHours(6)),
                Omsorgsdag(dato = LocalDate.parse("2020-02-01"), tid = Duration.ofHours(10)),
                Omsorgsdag(dato = LocalDate.parse("2020-02-03"), tid = Duration.ofHours(8)),
            )
        )
        val tidPerMåned = omsorgstilbud.beregnTidPerMåned().toString()
        val forventet = "[{måned=Januar, tid=16 timer}, {måned=Februar, tid=18 timer}]"
        assertEquals(forventet, tidPerMåned)
    }
}