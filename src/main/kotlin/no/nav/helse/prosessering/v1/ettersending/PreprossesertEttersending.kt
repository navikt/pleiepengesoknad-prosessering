package no.nav.helse.prosessering.v1.ettersending

import no.nav.helse.aktoer.AktoerId
import no.nav.helse.prosessering.v1.PreprossesertSoker
import java.net.URI
import java.time.ZonedDateTime

data class PreprossesertEttersending(
    val språk: String?,
    val søknadId: String,
    val dokumentUrls: List<List<URI>>,
    val mottatt: ZonedDateTime,
    val søker: PreprossesertSoker,
    val harForstattRettigheterOgPlikter: Boolean,
    val harBekreftetOpplysninger: Boolean,
    val beskrivelse: String,
    val søknadstype: String
    ) {
    internal constructor(
        melding: Ettersending,
        dokumentUrls: List<List<URI>>,
        sokerAktoerId: AktoerId
    ) : this(
        språk = melding.språk,
        søknadId = melding.søknadId,
        dokumentUrls = dokumentUrls,
        mottatt = melding.mottatt,
        søker = PreprossesertSoker(melding.søker, sokerAktoerId),
        beskrivelse = melding.beskrivelse,
        søknadstype = melding.søknadstype,
        harForstattRettigheterOgPlikter = melding.harForståttRettigheterOgPlikter,
        harBekreftetOpplysninger = melding.harBekreftetOpplysninger
    )
}

