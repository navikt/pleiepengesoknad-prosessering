package no.nav.helse.prosessering.v1.asynkron

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.kafka.TopicEntry
import no.nav.helse.pleiepengerKonfiguert
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.PreprossesertMeldingV1
import no.nav.k9.søknad.Søknad
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer

data class Journalfort(@JsonProperty("journalpostId") val journalpostId: String, val søknad: Søknad)
data class Cleanup(val metadata: Metadata, val melding: PreprossesertMeldingV1, val journalførtMelding: Journalfort)

internal data class Topic<V>(
    val name: String,
    val serDes : SerDes<V>
) {
    val keySerializer = StringSerializer()
    val keySerde = Serdes.String()
    val valueSerde = Serdes.serdeFrom(serDes, serDes)
}

internal object Topics {
    val MOTTATT_V2 = Topic(
        name = "dusseldorf.privat-pleiepengesoknad-mottatt-v2",
        serDes = MottattSoknadSerDes()
    )
    val PREPROSESSERT = Topic(
        name = "dusseldorf.privat-pleiepengesoknad-preprosessert",
        serDes = PreprossesertSerDes()
    )
    val CLEANUP = Topic(
        name = "dusseldorf.privat-pleiepengesoknad-cleanup",
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
private class CleanupSerDes: SerDes<TopicEntry<Cleanup>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<Cleanup>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}
