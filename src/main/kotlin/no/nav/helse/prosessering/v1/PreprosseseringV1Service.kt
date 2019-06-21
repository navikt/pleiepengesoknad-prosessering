package no.nav.helse.prosessering.v1

import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.SoknadId
import org.slf4j.LoggerFactory

internal class PreprosseseringV1Service {

    private companion object {
        private val logger = LoggerFactory.getLogger(PreprosseseringV1Service::class.java)

    }

    suspend internal fun preprosseser(
        melding: MeldingV1,
        metadata: Metadata,
        soknadId: SoknadId
    ) : PreprossesertMeldingV1 {
        logger.info("Preprosseserer.")
        return PreprossesertMeldingV1(
            soknadId = soknadId.id,
            relasjonTilBarnet = melding.relasjonTilBarnet
        )
    }
}