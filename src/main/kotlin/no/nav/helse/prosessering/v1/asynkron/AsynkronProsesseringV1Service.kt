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

    init {

        /* TODO: Starte Streams
            - PreprosseseringStream -> consumerer fra "privat-pleiepengesoknad-mottatt" og legger på "privat-pleiepengesoknad-preprosessert" (Lager PDF'er etc)
            - JournalforingStream -> consumerer fra "privat-pleiepengesoknad-preprosessert" og legger på "privat-pleiepengesoknad-journalfort"
            - GosysStream -> consumerer fra "privat-pleiepengesoknad-journalfort" >> END
        */
    }

    override suspend fun leggSoknadTilProsessering(
        melding: MeldingV1,
        metadata: Metadata
    ) : SoknadId {
        val soknadId = SoknadId.generate()
        logger.info("async")
        logger.info(soknadId.toString())
        // TODO: Format for "Utgeånde melding (DTO)"
        // TODO: Kafka producer som legger utgående melding på topic "privat-pleiepengesoknad-mottatt"
        return soknadId
    }

}