package no.nav.helse.prosessering.v1.asynkron

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.kafka.TopicEntry
import no.nav.helse.pleiepengerKonfiguert
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.PreprossesertMeldingV1
import no.nav.helse.prosessering.v1.asynkron.endringsmelding.EndringsmeldingV1
import no.nav.helse.prosessering.v1.asynkron.endringsmelding.PreprossesertEndringsmeldingV1
import no.nav.k9.søknad.Søknad
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer

data class Journalfort(@JsonProperty("journalpostId") val journalpostId: String, val søknad: Søknad)
data class Cleanup(val metadata: Metadata, val melding: PreprossesertMeldingV1, val journalførtMelding: Journalfort)
data class CleanupEndringsmelding(val metadata: Metadata, val melding: PreprossesertEndringsmeldingV1, val journalførtMelding: Journalfort)

data class Topic<V>(
    val name: String,
    val serDes : SerDes<V>
) {
    val keySerializer = StringSerializer()
    val keySerde = Serdes.String()
    val valueSerde = Serdes.serdeFrom(serDes, serDes)
}

internal object SøknadTopics {
    val MOTTATT = Topic(
        name = "dusseldorf.privat-pleiepengesoknad-mottatt",
        serDes = MottattSoknadSerDes()
    )
    val PREPROSSESERT = Topic(
        name = "dusseldorf.privat-pleiepengesoknad-preprosessert",
        serDes = PreprossesertSerDes()
    )
    val CLEANUP = Topic(
        name = "dusseldorf.privat-pleiepengesoknad-cleanup",
        serDes = CleanupSerDes()
    )
}

internal object EndringsmeldingTopics {
    val ENDRINGSMELDING_MOTTATT = Topic(
        name = "dusseldorf.privat-endringsmelding-pleiepenger-sykt-barn-mottatt",
        serDes = MottattEndringsmeldingSerDes()
    )
    val ENDRINGSMELDING_PREPROSSESERT = Topic(
        name = "dusseldorf.privat-endringsmelding-pleiepenger-sykt-barn-preprosessert",
        serDes = PreprossesertEndringsmeldingSerDes()
    )
    val ENDRINGSMELDING_CLEANUP = Topic(
        name = "dusseldorf.privat-endringsmelding-pleiepenger-sykt-barn-cleanup",
        serDes = CleanupEndringsmeldingSerDes()
    )
}

abstract class SerDes<V> : Serializer<V>, Deserializer<V> {
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

private class MottattEndringsmeldingSerDes: SerDes<TopicEntry<EndringsmeldingV1>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<EndringsmeldingV1>? {
        return data?.let {
            objectMapper.readValue<TopicEntry<EndringsmeldingV1>>(it)
        }
    }
}
private class PreprossesertEndringsmeldingSerDes: SerDes<TopicEntry<PreprossesertEndringsmeldingV1>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<PreprossesertEndringsmeldingV1>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}
private class CleanupEndringsmeldingSerDes: SerDes<TopicEntry<CleanupEndringsmelding>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<CleanupEndringsmelding>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}
