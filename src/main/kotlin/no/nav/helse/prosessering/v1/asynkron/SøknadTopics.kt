package no.nav.helse.prosessering.v1.asynkron

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.kafka.TopicEntry
import no.nav.helse.pleiepengerKonfiguert
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.PreprossesertMeldingV1
import no.nav.helse.prosessering.v1.asynkron.endringsmelding.EndringsmeldingMottatt
import no.nav.helse.prosessering.v1.asynkron.endringsmelding.EndringsmeldingV1
import no.nav.helse.prosessering.v1.asynkron.endringsmelding.PreprossesertEndringsmeldingV1
import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Organisasjonsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.felles.type.SøknadId
import no.nav.k9.søknad.ytelse.psb.v1.DataBruktTilUtledning
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9.søknad.ytelse.psb.v1.Uttak
import no.nav.k9.søknad.ytelse.psb.v1.UttakPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstaker
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidInfo
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.TilsynPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.Tilsynsordning
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

data class Journalfort(@JsonProperty("journalpostId") val journalpostId: String, val søknad: Søknad)
data class Cleanup(val metadata: Metadata, val melding: PreprossesertMeldingV1, val journalførtMelding: Journalfort)
data class CleanupEndringsmelding(
    val metadata: Metadata,
    val melding: PreprossesertEndringsmeldingV1,
    val journalførtMelding: Journalfort
)

data class Topic<V>(
    val name: String,
    val serDes: SerDes<V>
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

private class MottattSoknadSerDes : SerDes<TopicEntry<MeldingV1>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<MeldingV1>? {
        return data?.let {
            objectMapper.readValue<TopicEntry<MeldingV1>>(it)
        }
    }
}

private class PreprossesertSerDes : SerDes<TopicEntry<PreprossesertMeldingV1>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<PreprossesertMeldingV1>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}

private class CleanupSerDes : SerDes<TopicEntry<Cleanup>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<Cleanup>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}

private class MottattEndringsmeldingSerDes : SerDes<TopicEntry<EndringsmeldingV1>>() {
    private companion object {
        private val logger = LoggerFactory.getLogger(MottattEndringsmeldingSerDes::class.java)
    }

    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<EndringsmeldingV1>? {
        return data?.let {
            val readValue = objectMapper.readValue<TopicEntry<EndringsmeldingMottatt>>(it)
            val data = readValue.data
            val søknad = Søknad(
                SøknadId.of(UUID.randomUUID().toString()),
                Versjon.of("1.0.0"),
                ZonedDateTime.parse("2020-01-01T10:00:00Z"),
                Søker(NorskIdentitetsnummer.of("12345678910")),
                PleiepengerSyktBarn()
                    .medSøknadsperiode(Periode(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-10")))
                    .medSøknadInfo(DataBruktTilUtledning(true, true, true, true, true))
                    .medBarn(Barn(NorskIdentitetsnummer.of("10987654321"), null))
                    .medTilsynsordning(
                        Tilsynsordning().medPerioder(
                            mapOf(
                                Periode(
                                    LocalDate.parse("2020-01-01"),
                                    LocalDate.parse("2020-01-05")
                                ) to TilsynPeriodeInfo().medEtablertTilsynTimerPerDag(Duration.ofHours(8)),
                                Periode(
                                    LocalDate.parse("2020-01-06"),
                                    LocalDate.parse("2020-01-10")
                                ) to TilsynPeriodeInfo().medEtablertTilsynTimerPerDag(Duration.ofHours(4))
                            )
                        )
                    )
                    .medArbeidstid(
                        Arbeidstid().medArbeidstaker(
                            listOf(
                                Arbeidstaker(
                                    NorskIdentitetsnummer.of("12345678910"),
                                    Organisasjonsnummer.of("926032925"),
                                    ArbeidstidInfo(
                                        mapOf(
                                            Periode(
                                                LocalDate.parse("2018-01-01"),
                                                LocalDate.parse("2020-01-05")
                                            ) to ArbeidstidPeriodeInfo(Duration.ofHours(8), Duration.ofHours(4)),
                                            Periode(
                                                LocalDate.parse("2020-01-06"),
                                                LocalDate.parse("2020-01-10")
                                            ) to ArbeidstidPeriodeInfo(Duration.ofHours(8), Duration.ofHours(2))
                                        )
                                    )
                                )
                            )
                        )
                    )
                    .medUttak(
                        Uttak().medPerioder(
                            mapOf(
                                Periode(
                                    LocalDate.parse("2020-01-01"),
                                    LocalDate.parse("2020-01-05")
                                ) to UttakPeriodeInfo(Duration.ofHours(4)),
                                Periode(
                                    LocalDate.parse("2020-01-06"),
                                    LocalDate.parse("2020-01-10")
                                ) to UttakPeriodeInfo(Duration.ofHours(2))
                            )
                        )
                    )
            )
            val mottatt = data.copy(
                k9Format = JsonUtils.getObjectMapper().valueToTree(søknad) as ObjectNode
            )

            TopicEntry(
                readValue.metadata,
                EndringsmeldingV1(
                    søker = data.søker,
                    harBekreftetOpplysninger = data.harBekreftetOpplysninger,
                    harForståttRettigheterOgPlikter = data.harForståttRettigheterOgPlikter,
                    k9Format = Søknad.SerDes.deserialize(mottatt.k9Format)
                )
            )
        }
    }
}

private class PreprossesertEndringsmeldingSerDes : SerDes<TopicEntry<PreprossesertEndringsmeldingV1>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<PreprossesertEndringsmeldingV1>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}

private class CleanupEndringsmeldingSerDes : SerDes<TopicEntry<CleanupEndringsmelding>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<CleanupEndringsmelding>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}
