package no.nav.helse.k9format

import no.nav.helse.felles.*
import no.nav.helse.prosessering.v1.*
import no.nav.k9.søknad.JsonUtils
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class SelvstendigVirksomheterK9FormatTest {

    @Test
    fun `Går fint å bygge K9Format med tom liste av virksomheter`(){
        val selvstendigVirksomheter: List<Virksomhet> = listOf()
        val selvstendigVirksomheterK9Format = selvstendigVirksomheter.tilK9SelvstendigNæringsdrivende()
        val forventetJson = """
            [ ]
        """.trimIndent()
        assertEquals(forventetJson, JsonUtils.toString(selvstendigVirksomheterK9Format))
    }

    @Test
    fun `Bygge liste med to virksomheter til forventet K9Format`(){
        val selvstendigVirksomheter = listOf(
            Virksomhet(
                næringstyper = listOf(Næringstyper.ANNEN),
                fraOgMed = LocalDate.parse("2021-01-01"),
                tilOgMed = LocalDate.parse("2021-01-10"),
                navnPåVirksomheten = "Kjells Møbelsnekkeri",
                registrertINorge = true,
                yrkesaktivSisteTreFerdigliknedeÅrene = YrkesaktivSisteTreFerdigliknedeÅrene(LocalDate.parse("2021-01-01")),
                organisasjonsnummer = "111111"
            ), Virksomhet(
                næringstyper = listOf(Næringstyper.JORDBRUK_SKOGBRUK),
                organisasjonsnummer = "9999",
                fraOgMed = LocalDate.parse("2020-01-01"),
                navnPåVirksomheten = "Kjells Skogbruk",
                registrertINorge = false,
                registrertIUtlandet = Land(
                    "DEU",
                    "Tyskland"
                ),
                næringsinntekt = 900_000,
                regnskapsfører = Regnskapsfører(
                    "Bård",
                    "98989898"
                )
            )
        )
        val selvstendigVirksomheterK9Format = selvstendigVirksomheter.tilK9SelvstendigNæringsdrivende()
        val forventetJson = """
                [ {
                  "perioder" : {
                    "2021-01-01/2021-01-10" : {
                      "virksomhetstyper" : [ "ANNEN" ],
                      "erVarigEndring" : false,
                      "erNyoppstartet" : true,
                      "registrertIUtlandet" : false,
                      "landkode" : "NOR"
                    }
                  },
                  "organisasjonsnummer" : "111111",
                  "virksomhetNavn" : "Kjells Møbelsnekkeri"
                }, {
                  "perioder" : {
                    "2020-01-01/.." : {
                      "virksomhetstyper" : [ "JORDBRUK_SKOGBRUK" ],
                      "regnskapsførerNavn" : "Bård",
                      "regnskapsførerTlf" : "98989898",
                      "erVarigEndring" : false,
                      "bruttoInntekt" : 900000,
                      "erNyoppstartet" : true,
                      "registrertIUtlandet" : true,
                      "landkode" : "DEU"
                    }
                  },
                  "organisasjonsnummer" : "9999",
                  "virksomhetNavn" : "Kjells Skogbruk"
                } ]
        """.trimIndent()

        assertEquals(forventetJson, JsonUtils.toString(selvstendigVirksomheterK9Format))
    }

}
