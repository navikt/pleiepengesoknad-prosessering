package no.nav.helse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.prosessering.v1.Soker
import no.nav.helse.prosessering.v1.ettersending.Ettersending
import java.net.URI
import java.time.ZonedDateTime
import java.util.*

class EttersendingUtils {

    companion object {
        internal val objectMapper = jacksonObjectMapper().dusseldorfConfigured()

        private val gyldigFodselsnummerA = "02119970078"

        internal val default = Ettersending(
            søknadId = UUID.randomUUID().toString(),
            mottatt = ZonedDateTime.now(),
            språk = "no",
            søknadstype = "omsorgspenger",
            beskrivelse = "Beskrivelse av ettersending",
            søker = Soker(
                aktoerId = "123456",
                fodselsnummer = gyldigFodselsnummerA,
                fornavn = "Ola",
                etternavn = "Normann"
            ),
            vedleggUrls = listOf(
                URI("http://localhost:8080/vedlegg/1"),
                URI("http://localhost:8080/vedlegg/2"),
                URI("http://localhost:8080/vedlegg/3")
            ),
            harForståttRettigheterOgPlikter = true,
            harBekreftetOpplysninger = true
        )
    }

    internal fun Ettersending.somJson() = EttersendingUtils.objectMapper.writeValueAsString(this)
}
