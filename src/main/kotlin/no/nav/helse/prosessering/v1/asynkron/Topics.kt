package no.nav.helse.prosessering.v1.asynkron

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.PreprossesertMeldingV1
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer

data class TopicEntry<V>(val metadata: Metadata, val data: V)
internal data class Journalfort(val journalPostId: String, val melding: PreprossesertMeldingV1)
data class OppgaveOpprettet(val journalPostId: String, val oppgaveId: String, val melding: PreprossesertMeldingV1)

internal data class Topic<V>(
    val name: String,
    val serDes : SerDes<V>
) {
    val keySerializer = StringSerializer()
    val keySerde = Serdes.String()
    val valueSerde = Serdes.serdeFrom(serDes, serDes)
}

internal object Topics {
    val MOTTATT = Topic(
        name = "privat-pleiepengesoknad-mottatt",
        serDes = MottattSoknadSerDes()
    )
    val PREPROSSESERT = Topic(
        name = "privat-pleiepengesoknad-preprossesert",
        serDes = PreprossesertSerDes()
    )
    val JOURNALFORT = Topic(
        name = "privat-pleiepengesoknad-journalfort",
        serDes = JournalfortSerDes()
    )
    val OPPGAVE_OPPRETTET = Topic(
        name = "privat-pleiepengesoknad-oppgaveOpprettet",
        serDes = OppgaveOpprettetSerDes()
    )
}

internal abstract class SerDes<V> : Serializer<V>, Deserializer<V> {
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
private class PreprossesertSerDes: SerDes<TopicEntry<PreprossesertMeldingV1>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<PreprossesertMeldingV1>? {
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
private class OppgaveOpprettetSerDes: SerDes<TopicEntry<OppgaveOpprettet>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<OppgaveOpprettet>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}