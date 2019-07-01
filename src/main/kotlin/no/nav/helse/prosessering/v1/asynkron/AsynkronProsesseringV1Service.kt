package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.SoknadId
import no.nav.helse.prosessering.v1.PreprosseseringV1Service
import no.nav.helse.prosessering.v1.ProsesseringV1Service
import org.slf4j.LoggerFactory
import java.util.*

internal class AsynkronProsesseringV1Service(
    kafkaProducerProperties : Properties,
    kafkaStreamsProperties : Properties,
    preprosseseringV1Service: PreprosseseringV1Service
) : ProsesseringV1Service {

    private companion object {
        private val logger = LoggerFactory.getLogger(AsynkronProsesseringV1Service::class.java)
    }

    private val producer = SoknadProducer(kafkaProducerProperties)


    init {
        PreprosseseringStream(
            kafkaProperties = kafkaStreamsProperties,
            preprosseseringV1Service = preprosseseringV1Service
        )
    }

    override suspend fun leggSoknadTilProsessering(
        melding: MeldingV1,
        metadata: Metadata
    ) : SoknadId {
        val soknadId = SoknadId.generate()
        producer.produce(
            soknadId = soknadId,
            metadata = metadata,
            melding = melding
        )
        return soknadId
    }

}