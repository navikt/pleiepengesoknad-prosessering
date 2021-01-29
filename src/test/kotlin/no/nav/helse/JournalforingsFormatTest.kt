package no.nav.helse

import no.nav.helse.dokument.JournalforingsFormat
import no.nav.helse.felles.*
import no.nav.helse.prosessering.v1.*
import org.skyscreamer.jsonassert.JSONAssert
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.Test

class JournalforingsFormatTest {

    @Test
    fun `Soknaden journalfoeres som JSON uten vedlegg`() {
        val soknadId = UUID.randomUUID().toString()
        val json = JournalforingsFormat.somJson(melding(soknadId))
        println(String(json))
        JSONAssert.assertEquals("""
        {
            "språk": null,
            "søknadId": "$soknadId",
            "mottatt": "2018-01-02T03:04:05.000000006Z",
            "fraOgMed": "2018-01-01",
            "tilOgMed": "2018-02-02",
            "vedleggUrls": [
                "http://localhost:8080/1234", "http://localhost:8080/12345"],
            "søker": {
                "aktørId": "123456",
                "fødselsnummer": "1212",
                "fornavn": "Ola",
                "mellomnavn": "Mellomnavn",
                "etternavn": "Nordmann"
            },
            "barn": {
                "fødselsnummer": "2323",
                "navn": "Kari",
                "fødselsdato": null,
                "aktørId": null
            },
            "arbeidsgivere": {
                "organisasjoner": [{
                    "organisasjonsnummer": "1212",
                    "navn": "Nei",
                    "skalJobbe": "nei",
                    "jobberNormaltTimer": 0.0,
                    "skalJobbeProsent": 0.0,
                    "vetIkkeEkstrainfo": null,
                },{
                    "organisasjonsnummer": "54321",
                    "navn": "Navn",
                    "skalJobbe": "redusert",
                    "skalJobbeProsent": 22.512,
                    "vetIkkeEkstrainfo": null,
                    "jobberNormaltTimer": 0.0,
                }]
            },
            "medlemskap": {
                "harBoddIUtlandetSiste12Mnd": true,
                "utenlandsoppholdNeste12Mnd": [],
                "skalBoIUtlandetNeste12Mnd": true,
                "utenlandsoppholdSiste12Mnd": []
            },
            "harMedsøker": true,
            "samtidigHjemme": null,
            "bekrefterPeriodeOver8Uker": true,
            "harBekreftetOpplysninger" : true,
	        "harForståttRettigheterOgPlikter": true,
            "tilsynsordning": {
                "svar": "ja",
                "ja": {
                    "mandag": "PT5H",
                    "tirsdag": "PT4H",
                    "onsdag": "PT3H45M",
                    "torsdag": "PT2H",
                    "fredag": "PT1H30M",
                    "tilleggsinformasjon": "Litt tilleggsinformasjon."
                },
                "vetIkke": null
            },
            "beredskap": {
                "beredskap": true,
                "tilleggsinformasjon": "I Beredskap",
            },
            "nattevåk": {
                "harNattevåk": true,
                "tilleggsinformasjon": "Har Nattevåk"
            },
             "utenlandsoppholdIPerioden": {
                "skalOppholdeSegIUtlandetIPerioden": false,
                "opphold": []
            },
          "ferieuttakIPerioden": {
            "skalTaUtFerieIPerioden": false,
            "ferieuttak": [
            ]
          },
            "frilans": {
              "startdato": "2018-02-01",
              "jobberFortsattSomFrilans": true
            },
            "selvstendigVirksomheter" : [],
          "skalBekrefteOmsorg": true,
          "skalPassePaBarnetIHelePerioden": true,
          "beskrivelseOmsorgsrollen": "En kort beskrivelse",
          "barnRelasjon" : "FAR",
          "barnRelasjonBeskrivelse" : null
        }
        """.trimIndent(), String(json), true)

    }

    private fun melding(soknadId: String) : MeldingV1 = MeldingV1(
        søknadId = soknadId,
        mottatt = ZonedDateTime.of(2018,1,2,3,4,5,6, ZoneId.of("UTC")),
        fraOgMed = LocalDate.parse("2018-01-01"),
        tilOgMed = LocalDate.parse("2018-02-02"),
        søker = Søker(
            aktørId = "123456",
            fødselsnummer = "1212",
            etternavn = "Nordmann",
            mellomnavn = "Mellomnavn",
            fornavn = "Ola"
        ),
        barn = Barn(
            navn = "Kari",
            fødselsnummer = "2323",
            fødselsdato = null,
            aktørId = null
        ),
        bekrefterPeriodeOver8Uker = true,
        arbeidsgivere = Arbeidsgivere(
            organisasjoner = listOf(
                Organisasjon("1212", "Nei", jobberNormaltTimer = 0.0, skalJobbeProsent = 0.0, vetIkkeEkstrainfo = null, skalJobbe = "nei"),
                Organisasjon("54321", "Navn", skalJobbeProsent = 22.512, jobberNormaltTimer = 0.0, vetIkkeEkstrainfo = null, skalJobbe = "redusert")
            )
        ),
        vedleggUrls = listOf(
            URI("http://localhost:8080/1234"),
            URI("http://localhost:8080/12345")
        ),
        medlemskap = Medlemskap(
            harBoddIUtlandetSiste12Mnd = true,
            skalBoIUtlandetNeste12Mnd = true
        ),
        harMedsøker = true,
        harBekreftetOpplysninger = true,
        harForståttRettigheterOgPlikter = true,
        tilsynsordning = Tilsynsordning(
            svar = "ja",
            ja = TilsynsordningJa(
                mandag = Duration.ofHours(5),
                tirsdag = Duration.ofHours(4),
                onsdag = Duration.ofHours(3).plusMinutes(45),
                torsdag = Duration.ofHours(2),
                fredag = Duration.ofHours(1).plusMinutes(30),
                tilleggsinformasjon = "Litt tilleggsinformasjon."
            ),
            vetIkke = null
        ),
        beredskap = Beredskap(
            beredskap = true,
            tilleggsinformasjon = "I Beredskap"
        ),
        nattevåk = Nattevåk(
            harNattevåk = true,
            tilleggsinformasjon = "Har Nattevåk"
        ),
        utenlandsoppholdIPerioden = UtenlandsoppholdIPerioden(skalOppholdeSegIUtlandetIPerioden = false, opphold = listOf()),
        ferieuttakIPerioden = FerieuttakIPerioden(skalTaUtFerieIPerioden = false, ferieuttak = listOf()),
        frilans = Frilans(
            startdato = LocalDate.parse("2018-02-01"),
            jobberFortsattSomFrilans = true
        ),
        selvstendigVirksomheter = listOf(),
        skalBekrefteOmsorg = true,
        skalPassePaBarnetIHelePerioden = true,
        beskrivelseOmsorgsrollen = "En kort beskrivelse",
        barnRelasjon = BarnRelasjon.FAR
    )
}
