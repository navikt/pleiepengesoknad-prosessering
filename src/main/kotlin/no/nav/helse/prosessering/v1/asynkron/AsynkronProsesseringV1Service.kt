package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.SoknadId
import no.nav.helse.prosessering.v1.ProsesseringV1Service
import org.slf4j.LoggerFactory

class AsynkronProsesseringV1Service() : ProsesseringV1Service {
    private companion object {
        private val logger = LoggerFactory.getLogger(AsynkronProsesseringV1Service::class.java)
    }

    override suspend fun leggSoknadTilProsessering(
        melding: MeldingV1,
        metadata: Metadata
    ) : SoknadId {
        val soknadId = SoknadId.generate()
        logger.info("async")
        logger.info(soknadId.toString())
        return soknadId
    }

}