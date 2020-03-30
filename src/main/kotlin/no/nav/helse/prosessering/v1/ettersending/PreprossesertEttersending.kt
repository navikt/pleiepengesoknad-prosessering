package no.nav.helse.prosessering.v1.ettersending

import no.nav.helse.aktoer.AktoerId
import no.nav.helse.prosessering.v1.PreprossesertSoker
import java.net.URI
import java.time.ZonedDateTime

data class PreprossesertEttersending(
    val sprak: String?,
    val soknadId: String,
    val dokumentUrls: List<List<URI>>,
    val mottatt: ZonedDateTime,
    val soker: PreprossesertSoker,
    val harForstattRettigheterOgPlikter: Boolean,
    val harBekreftetOpplysninger: Boolean,
    val beskrivelse: String,
    val soknadstype: String
    ) {
    internal constructor(
        melding: Ettersending,
        dokumentUrls: List<List<URI>>,
        sokerAktoerId: AktoerId
    ) : this(
        sprak = melding.sprak,
        soknadId = melding.soknadId,
        dokumentUrls = dokumentUrls,
        mottatt = melding.mottatt,
        soker = PreprossesertSoker(melding.soker, sokerAktoerId),
        beskrivelse = melding.beskrivelse,
        soknadstype = melding.soknadstype,
        harForstattRettigheterOgPlikter = melding.harForstattRettigheterOgPlikter,
        harBekreftetOpplysninger = melding.harBekreftetOpplysninger
    )
}

