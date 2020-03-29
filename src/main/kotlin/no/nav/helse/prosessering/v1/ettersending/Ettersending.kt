package no.nav.helse.prosessering.v1.ettersending

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.helse.prosessering.v1.Soker
import java.net.URI
import java.time.ZonedDateTime

data class Ettersending(
    val søker : Soker,
    val søknadId: String,
    val mottatt: ZonedDateTime,
    val språk: String,
    @JsonProperty("vedlegg_urls")
    val vedleggUrls: List<URI>,
    val harForståttRettigheterOgPlikter: Boolean,
    val harBekreftetOpplysninger: Boolean,
    val beskrivelse: String,
    val søknadstype: String
)
