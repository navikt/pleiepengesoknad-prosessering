package no.nav.helse.prosessering.v1.ettersending

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.helse.prosessering.v1.Soker
import java.net.URI
import java.time.ZonedDateTime

data class Ettersending(
    val soker : Soker,
    val soknadId: String,
    val mottatt: ZonedDateTime,
    val sprak: String,
    @JsonProperty("vedlegg_urls")
    val vedleggUrls: List<URI>,
    val harForstattRettigheterOgPlikter: Boolean,
    val harBekreftetOpplysninger: Boolean,
    val beskrivelse: String,
    val soknadstype: String
)
