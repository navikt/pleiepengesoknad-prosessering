package no.nav.helse

import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.prosessering.v1.asynkron.Topics.JOURNALFORT
import no.nav.helse.prosessering.v1.asynkron.Topics.MOTTATT
import no.nav.helse.prosessering.v1.asynkron.Topics.OPPGAVE_OPPRETTET
import no.nav.helse.prosessering.v1.asynkron.Topics.PREPROSSESERT

private const val username = "srvkafkaclient"
private const val password = "kafkaclient"

object KafkaWrapper {
    fun bootstrap() : KafkaEnvironment {
        val kafkaEnvironment = KafkaEnvironment(
            users = listOf(JAASCredential(username, password)),
            autoStart = true,
            withSchemaRegistry = false,
            withSecurity = true,
            topicNames= listOf(
                MOTTATT.name,
                PREPROSSESERT.name,
                JOURNALFORT.name,
                OPPGAVE_OPPRETTET.name
            )
        )
        return kafkaEnvironment
    }
}

fun KafkaEnvironment.username() = username
fun KafkaEnvironment.password() = password