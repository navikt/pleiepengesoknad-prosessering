package no.nav.helse

import no.nav.helse.dokument.JournalforingsFormat
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
        JSONAssert.assertEquals("""
        {
            "sprak": null,
            "soknad_id": "$soknadId",
            "mottatt": "2018-01-02T03:04:05.000000006Z",
            "fra_og_med": "2018-01-01",
            "til_og_med": "2018-02-02",
            "soker": {
                "aktoer_id": "123456",
                "fodselsnummer": "1212",
                "fornavn": "Ola",
                "mellomnavn": "Mellomnavn",
                "etternavn": "Nordmann"
            },
            "barn": {
                "fodselsnummer": "2323",
                "navn": "Kari",
                "fodselsdato": null,
                "aktoer_id": null
            },
            "relasjon_til_barnet": "Mor",
            "arbeidsgivere": {
                "organisasjoner": [{
                    "organisasjonsnummer": "1212",
                    "navn": "Nei",
                    "skal_jobbe": "nei",
                    "jobber_normalt_timer": 0.0,
                    "skal_jobbe_prosent": 0.0,
                    "vet_ikke_ekstrainfo": null,
                },{
                    "organisasjonsnummer": "54321",
                    "navn": "Navn",
                    "skal_jobbe": "redusert",
                    "skal_jobbe_prosent": 22.512,
                    "vet_ikke_ekstrainfo": null,
                    "jobber_normalt_timer": 0.0,
                }]
            },
            "medlemskap": {
                "har_bodd_i_utlandet_siste_12_mnd": true,
                "utenlandsopphold_neste_12_mnd": [],
                "skal_bo_i_utlandet_neste_12_mnd": true,
                "utenlandsopphold_siste_12_mnd": []
            },
            "har_medsoker": true,
            "samtidig_hjemme": null,
            "bekrefter_periode_over_8_uker": true,
            "har_bekreftet_opplysninger" : true,
	        "har_forstatt_rettigheter_og_plikter": true,
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
                "vet_ikke": null
            },
            "beredskap": {
                "i_beredskap": true,
                "tilleggsinformasjon": "I Beredskap",
            },
            "nattevaak": {
                "har_nattevaak": true,
                "tilleggsinformasjon": "Har Nattevåk"
            },
             "utenlandsopphold_i_perioden": {
                "skal_oppholde_seg_i_utlandet_i_perioden": false,
                "opphold": []
            },
          "ferieuttak_i_perioden": {
            "skal_ta_ut_ferie_i_periode": false,
            "ferieuttak": [
            ]
          },
            "frilans": {
              "startdato": "2018-02-01",
              "jobber_fortsatt_som_frilans": true
            },
            "selvstendig_virksomheter" : [],
          "skal_bekrefte_omsorg": true,
          "skal_passe_pa_barnet_i_hele_perioden": true,
          "beskrivelse_omsorgsrollen": "En kort beskrivelse"
        }
        """.trimIndent(), String(json), true)

    }

    private fun melding(soknadId: String) : MeldingV1 = MeldingV1(
        soknadId = soknadId,
        mottatt = ZonedDateTime.of(2018,1,2,3,4,5,6, ZoneId.of("UTC")),
        fraOgMed = LocalDate.parse("2018-01-01"),
        tilOgMed = LocalDate.parse("2018-02-02"),
        soker = Soker(
            aktoerId = "123456",
            fodselsnummer = "1212",
            etternavn = "Nordmann",
            mellomnavn = "Mellomnavn",
            fornavn = "Ola"
        ),
        barn = Barn(
            navn = "Kari",
            fodselsnummer = "2323",
            fodselsdato = null,
            aktoerId = null
        ),
        relasjonTilBarnet = "Mor",
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
        harMedsoker = true,
        harBekreftetOpplysninger = true,
        harForstattRettigheterOgPlikter = true,
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
        nattevaak = Nattevaak(
            harNattevaak = true,
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
        beskrivelseOmsorgsRollen = "En kort beskrivelse"
    )
}
