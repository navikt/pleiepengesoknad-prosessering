package no.nav.helse.prosessering.v1.asynkron

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.PreprossesertMeldingV1
import no.nav.helse.prosessering.v1.ettersending.Ettersending
import no.nav.helse.prosessering.v1.ettersending.PreprossesertEttersending
import no.nav.k9.ettersendelse.Ettersendelse
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer

data class TopicEntry<V>(val metadata: Metadata, val data: V)
data class Journalfort(@JsonProperty("journalpostId") val journalpostId: String, val søknad: JsonNode)
data class Cleanup(val metadata: Metadata, val melding: PreprossesertMeldingV1, val journalførtMelding: Journalfort)

data class CleanupEttersending(val metadata: Metadata, val melding: PreprossesertEttersending, val journalførtMelding: JournalførtEttersending)
data class JournalførtEttersending(@JsonProperty("journalpostId") val journalpostId: String, val søknad: Ettersendelse)//TODO:Egen søknad for ettersending

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
    val CLEANUP = Topic(
        name = "privat-pleiepengesoknad-cleanup",
        serDes = CleanupSerDes()
    )
    val ETTERSENDING_MOTTATT = Topic(
        name = "privat-pleiepengesoknad-ettersending-mottatt",
        serDes = MottattEttersendingSerDes()
    )
    val ETTERSENDING_PREPROSSESERT = Topic(
        name = "privat-pleiepengesoknad-ettersending-preprossesert",
        serDes = PreprossesertEttersendingSerDes()
    )
    val ETTERSENDING_CLEANUP = Topic(
        name = "privat-pleiepengesoknad-ettersending-cleanup",
        serDes = CleanupSerDesEttersending()
    )
    val ETTERSENDING_JOURNALFORT = Topic(
        name = "privat-k9-digital-ettersendelse-journalfort",
        serDes = JournalfortSerDesEttersending()
    )
}

internal abstract class SerDes<V> : Serializer<V>, Deserializer<V> {
    protected val objectMapper = jacksonObjectMapper()
        .dusseldorfConfigured()
        .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
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
private class CleanupSerDes: SerDes<TopicEntry<Cleanup>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<Cleanup>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}

private class MottattEttersendingSerDes: SerDes<TopicEntry<Ettersending>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<Ettersending>? {
        return data?.let {
            objectMapper.readValue<TopicEntry<Ettersending>>(it)
        }
    }
}

private class PreprossesertEttersendingSerDes: SerDes<TopicEntry<PreprossesertEttersending>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<PreprossesertEttersending>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}

private class CleanupSerDesEttersending: SerDes<TopicEntry<CleanupEttersending>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<CleanupEttersending>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}
private class JournalfortSerDesEttersending: SerDes<TopicEntry<JournalførtEttersending>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<JournalførtEttersending>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}
