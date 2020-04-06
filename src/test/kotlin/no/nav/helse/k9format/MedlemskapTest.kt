package no.nav.helse.k9format

import no.nav.helse.prosessering.v1.Bosted
import no.nav.helse.prosessering.v1.Medlemskap
import no.nav.k9.søknad.felles.Landkode
import no.nav.k9.søknad.felles.Periode
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

internal class MedlemskapTest  {

    @Test
    internal fun `Overlappende datoer på tvers av harBodd og skalBo`() {
        val medlemskap = Medlemskap(
            harBoddIUtlandetSiste12Mnd = true,
            skalBoIUtlandetNeste12Mnd = true,
            utenlandsoppholdSiste12Mnd = listOf(
                Bosted(
                    fraOgMed = LocalDate.parse("2020-01-01"),
                    tilOgMed = LocalDate.parse("2020-01-20"),
                    landkode = "foo",
                    landnavn = "bar"
                )
            ),
            utenlandsoppholdNeste12Mnd = listOf(
                Bosted(
                    fraOgMed = LocalDate.parse("2020-01-20"),
                    tilOgMed = LocalDate.parse("2020-01-25"),
                    landkode = "foo2",
                    landnavn = "bar2"
                )
            )
        )

        val bosteder = medlemskap.tilK9bosteder()
        assertEquals(2, bosteder.perioder.size)
        assertEquals(bosteder.perioder[Periode.parse("2020-01-01/2020-01-20")]!!.land, Landkode.of("foo"))
        assertEquals(bosteder.perioder[Periode.parse("2020-01-21/2020-01-25")]!!.land, Landkode.of("foo2"))
    }

    @Test
    internal fun `Overlappende datoer på tvers av harBodd og skalBo med enkeltdager`() {
        val dato = LocalDate.parse("2020-01-01")
        val medlemskap = Medlemskap(
            harBoddIUtlandetSiste12Mnd = true,
            skalBoIUtlandetNeste12Mnd = true,
            utenlandsoppholdSiste12Mnd = listOf(
                Bosted(
                    fraOgMed = dato,
                    tilOgMed = dato,
                    landkode = "foo",
                    landnavn = "bar"
                )
            ),
            utenlandsoppholdNeste12Mnd = listOf(
                Bosted(
                    fraOgMed = dato,
                    tilOgMed = dato,
                    landkode = "foo2",
                    landnavn = "bar2"
                )
            )
        )

        val bosteder = medlemskap.tilK9bosteder()
        assertEquals(2, bosteder.perioder.size)
        assertEquals(bosteder.perioder[Periode.parse("2020-01-01/2020-01-01")]!!.land, Landkode.of("foo"))
        assertEquals(bosteder.perioder[Periode.parse("2020-01-02/2020-01-02")]!!.land, Landkode.of("foo2"))
    }

    @Test
    internal fun `Ingen overlapp mellom harBodd og skalBo`() {
        val medlemskap = Medlemskap(
            harBoddIUtlandetSiste12Mnd = true,
            skalBoIUtlandetNeste12Mnd = true,
            utenlandsoppholdSiste12Mnd = listOf(
                Bosted(
                    fraOgMed = LocalDate.parse("2020-01-01"),
                    tilOgMed = LocalDate.parse("2020-01-20"),
                    landkode = "foo",
                    landnavn = "bar"
                )
            ),
            utenlandsoppholdNeste12Mnd = listOf(
                Bosted(
                    fraOgMed = LocalDate.parse("2020-01-21"),
                    tilOgMed = LocalDate.parse("2020-01-21"),
                    landkode = "foo2",
                    landnavn = "bar2"
                )
            )
        )

        val bosteder = medlemskap.tilK9bosteder()
        assertEquals(2, bosteder.perioder.size)
        assertEquals(bosteder.perioder[Periode.parse("2020-01-01/2020-01-20")]!!.land, Landkode.of("foo"))
        assertEquals(bosteder.perioder[Periode.parse("2020-01-21/2020-01-21")]!!.land, Landkode.of("foo2"))
    }
}