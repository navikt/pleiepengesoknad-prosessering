package no.nav.helse

import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.prosessering.v1.asynkron.Topics.JOURNALFORT
import no.nav.helse.prosessering.v1.asynkron.Topics.MOTTATT
import no.nav.helse.prosessering.v1.asynkron.Topics.PREPROSSESERT
import org.apache.kafka.common.acl.AccessControlEntry
import org.apache.kafka.common.acl.AclBinding
import org.apache.kafka.common.acl.AclOperation
import org.apache.kafka.common.acl.AclPermissionType
import org.apache.kafka.common.resource.PatternType
import org.apache.kafka.common.resource.ResourcePattern
import org.apache.kafka.common.resource.ResourceType

//private const val username = "srvpps-prosessering"
//private const val password = "password"

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
                JOURNALFORT.name
            )
        )
//        val  a = kafkaEnvironment.adminClient!!.createAcls(createACL(mapOf(
//            MOTTATT.name to username,
//            PREPROSSESERT.name to username,
//            JOURNALFORT.name to username
//        ))).all().get()

        return kafkaEnvironment
    }
}

private fun createACL(topicUser: Map<String, String>): List<AclBinding> =
    topicUser.flatMap {
        val (topic, user) = it

        listOf(AclOperation.DESCRIBE, AclOperation.WRITE, AclOperation.CREATE, AclOperation.READ).let { lOp ->

            val tPattern = ResourcePattern(ResourceType.TOPIC, topic, PatternType.LITERAL)
            val gPattern = ResourcePattern(ResourceType.GROUP, "*", PatternType.LITERAL)

            val principal = "User:$user"
            val host = "*"
            val allow = AclPermissionType.ALLOW

            lOp.map { op ->
                AclBinding(tPattern, AccessControlEntry(principal, host, op, allow))
            } + AclBinding(gPattern, AccessControlEntry(principal, host, AclOperation.READ, allow))
        }
    }


fun KafkaEnvironment.username() = username
fun KafkaEnvironment.password() = password