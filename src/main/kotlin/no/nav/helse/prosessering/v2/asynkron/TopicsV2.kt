package no.nav.helse.prosessering.v2.asynkron

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.kafka.TopicEntry
import no.nav.helse.pleiepengerKonfiguert
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.v2.MeldingV2
import no.nav.helse.prosessering.v2.PreprossesertMeldingV2
import no.nav.k9.søknad.Søknad
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer

data class JournalfortV2(@JsonProperty("journalpostId") val journalpostId: String, val søknad: Søknad)
data class CleanupV2(val metadata: Metadata, val melding: PreprossesertMeldingV2, val journalførtMelding: JournalfortV2)

internal data class Topic<V>(
    val name: String,
    val serDes : SerDes<V>
) {
    val keySerializer = StringSerializer()
    val keySerde = Serdes.String()
    val valueSerde = Serdes.serdeFrom(serDes, serDes)
}

internal object TopicsV2 {
    val MOTTATT = Topic(
        name = "privat-pleiepengesoknad-v2-mottatt",
        serDes = MottattSoknadSerDes()
    )
    val PREPROSSESERT = Topic(
        name = "privat-pleiepengesoknad-v2-preprossesert",
        serDes = PreprossesertSerDes()
    )
    val JOURNALFORT = Topic(
        name = "privat-pleiepengesoknad-v2-journalfort",
        serDes = JournalfortSerDes()
    )
    val CLEANUP = Topic(
        name = "privat-pleiepengesoknad-v2-cleanup",
        serDes = CleanupSerDes()
    )
}

internal abstract class SerDes<V> : Serializer<V>, Deserializer<V> {
    protected val objectMapper = jacksonObjectMapper().pleiepengerKonfiguert()

    override fun serialize(topic: String?, data: V): ByteArray? {
        return data?.let {
            objectMapper.writeValueAsBytes(it)
        }
    }
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
}
private class MottattSoknadSerDes: SerDes<TopicEntry<MeldingV2>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<MeldingV2>? {
        return data?.let {
            objectMapper.readValue<TopicEntry<MeldingV2>>(it)
        }
    }
}
private class PreprossesertSerDes: SerDes<TopicEntry<PreprossesertMeldingV2>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<PreprossesertMeldingV2>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}
private class JournalfortSerDes: SerDes<TopicEntry<JournalfortV2>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<JournalfortV2>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}
private class CleanupSerDes: SerDes<TopicEntry<CleanupV2>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<CleanupV2>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}
