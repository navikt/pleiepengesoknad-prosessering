package no.nav.helse.kafka

import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.Topology
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

internal class ManagedKafkaStreams(
    private val name: String,
    topology: Topology,
    properties: Properties
) : HealthCheck {

    private companion object {
        private const val unhealthyEtterStoppetIMinutter = 15L
    }

    override suspend fun check(): Result {
        return when(kafkaStreams.state()) {
            KafkaStreams.State.PENDING_SHUTDOWN, KafkaStreams.State.NOT_RUNNING -> {
                val stoppedAt = stopped?: LocalDateTime.now().minusMinutes(unhealthyEtterStoppetIMinutter + 1)
                val stoppedInMinutes = Duration.between(stoppedAt, LocalDateTime.now()).toMinutes()
                if (stoppedInMinutes >= unhealthyEtterStoppetIMinutter) UnHealthy(name, "Stream har vært stoppet i $stoppedInMinutes minutter.")
                else Healthy(name, "Stream har vært stoppet i $stoppedInMinutes minutter. Meldes ut først etter $unhealthyEtterStoppetIMinutter minutter.")
            }
            KafkaStreams.State.RUNNING, KafkaStreams.State.REBALANCING, KafkaStreams.State.CREATED -> {
                Healthy(name, "Kjører som normalt i state ${kafkaStreams.state().name}.")
            }
            else -> UnHealthy(name, "Stream befinner seg i state '${kafkaStreams.state().name}'.")
        }
    }

    private val log = LoggerFactory.getLogger("no.nav.$name.stream")
    private var kafkaStreams = managed(KafkaStreams(topology, properties))
    private var stopped : LocalDateTime? = null

    init {
        start()
    }

    private fun start() {
        log.info("Starter")
        kafkaStreams.start()
    }

    internal fun stop() {
        when (kafkaStreams.state()) {
            KafkaStreams.State.PENDING_SHUTDOWN, KafkaStreams.State.NOT_RUNNING -> log.info("Stoppes allerede. er i state ${kafkaStreams.state().name}")
            else -> {
                stopped = LocalDateTime.now()
                log.info("Stopper fra state ${kafkaStreams.state().name}")
                kafkaStreams.close()
            }
        }
    }

    private fun managed(streams: KafkaStreams) : KafkaStreams{
        streams.setStateListener { newState, oldState ->
            log.info("Stream endret state fra $oldState til $newState")
            if (newState == KafkaStreams.State.ERROR) {
                stop()
            }
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            stop()
        })

        return streams
    }
}