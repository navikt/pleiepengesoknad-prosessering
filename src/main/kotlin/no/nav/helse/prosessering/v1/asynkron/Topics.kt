package no.nav.helse.prosessering.v1.asynkron

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.gosys.JournalPostId
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.UtgaendeMeldingV1
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.MDC

internal data class TopicEntry<V>(
    val metadata: Metadata,
    val data: V
)

internal data class Journalfort(val journalPostId: JournalPostId, val melding: UtgaendeMeldingV1)

internal data class Topic<V>(
    val name: String,
    val valueSerde: Serde<TopicEntry<V>>
) {
    val keySerde = Serdes.String()
}

private val MOTTATT_SERDES = MottattSoknadSerDes()
private val PREPROSSESERT_SERDES = PreprossesertSerDes()
private val JOURNALFORT_SERDES = JournalfortSerDes()

internal object Topics {
    val MOTTATT = Topic(
        name = "privat-pleiepengesoknad-mottatt",
        valueSerde = Serdes.serdeFrom(MOTTATT_SERDES, MOTTATT_SERDES)
    )
    val PREPROSSESERT = Topic(
        name = "privat-pleiepengesoknad-preprossesert",
        valueSerde = Serdes.serdeFrom(PREPROSSESERT_SERDES, PREPROSSESERT_SERDES)
    )
    val JOURNALFORT = Topic(
        name = "privat-pleiepengesoknad-journalfort",
        valueSerde = Serdes.serdeFrom(JOURNALFORT_SERDES, JOURNALFORT_SERDES)
    )
}

private abstract class SerDes<V> : Serializer<V>, Deserializer<V> {
    protected val objectMapper = jacksonObjectMapper().dusseldorfConfigured()
    override fun serialize(topic: String?, data: V): ByteArray? {
        return data?.let {
            objectMapper.writeValueAsBytes(it)
        }
    }
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
}
private class MottattSoknadSerDes: SerDes<TopicEntry<MeldingV1>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<MeldingV1>? {
        return data?.let {
            objectMapper.readValue<TopicEntry<MeldingV1>>(it)
        }
    }

}
private class PreprossesertSerDes: SerDes<TopicEntry<UtgaendeMeldingV1>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<UtgaendeMeldingV1>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }

}
private class JournalfortSerDes: SerDes<TopicEntry<Journalfort>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<Journalfort>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}
internal fun <BEFORE, AFTER>runBlockingWithMDC(soknadId: String, entry: TopicEntry<BEFORE>, block: suspend() -> AFTER) : AFTER {
    return runBlocking(MDCContext()) {
        MDC.put("correlation_id", entry.metadata.correlationId)
        MDC.put("request_id", entry.metadata.requestId)
        MDC.put("soknad_id", soknadId)
        block()
    }
}
