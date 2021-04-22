package no.nav.helse.k9format

import no.nav.helse.prosessering.v1.Arbeidsform
import no.nav.helse.prosessering.v1.Arbeidsgivere
import no.nav.helse.prosessering.v1.Organisasjon
import no.nav.helse.prosessering.v1.SkalJobbe
import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.felles.Periode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ArbeidTest  {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ArbeidTest::class.java)
        val arbeidsgivere = Arbeidsgivere(
            listOf(
                Organisasjon(
                    "917755736",
                    "Gyldig",
                    jobberNormaltTimer = 4.0,
                    skalJobbeProsent = 50.0,
                    skalJobbe = SkalJobbe.REDUSERT,
                    arbeidsform = Arbeidsform.VARIERENDE
                )
            )
        )
    }

    @Test
    internal fun `Konvertering til Arbeid uten info om snf gir Arbeid med selvstendigNæringsdrivende og frilanser med tom liste`() {
        val k9Arbeid = arbeidsgivere.tilK9Arbeid(
            frilans = null,
            selvstendigVirksomheter = listOf(),
            søknadsPeriode = Periode.builder()
                .fraOgMed(LocalDate.parse("2020-10-06"))
                .tilOgMed(LocalDate.parse("2020-10-08"))
                .build()
        )

        val json = JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(k9Arbeid)
        logger.info(json)
        assertEquals(
            //language=json
            """
            {
              "arbeidstaker" : [ {
                "norskIdentitetsnummer" : null,
                "organisasjonsnummer" : "917755736",
                "perioder" : {
                  "2020-10-06/2020-10-08" : {
                    "skalJobbeProsent" : 50.00,
                    "jobberNormaltPerUke" : "PT4H"
                  }
                }
              } ],
              "selvstendigNæringsdrivende" : [ ],
              "frilanser" : [ ]
            }
        """.trimIndent(), json)
    }
}
