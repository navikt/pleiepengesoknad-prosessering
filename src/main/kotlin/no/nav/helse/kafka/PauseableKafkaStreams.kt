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
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer

internal class PauseableKafkaStreams(
    private val name: String,
    private val topology: Topology,
    private val properties: Properties,
    private val considerRestartEvery : Duration = Duration.ofMinutes(defaultConsiderRestartEveryInMinutes),
    private val considerRestart: (pausedAt: LocalDateTime, reason: Throwable) -> Boolean = { pausedAt, _ -> pausedAt.isBefore(LocalDateTime.now().minusMinutes(5)) },
    private val pauseOn: (throwable: Throwable) -> Boolean
) : HealthCheck {
    override suspend fun check(): Result {
        val isPaused = pausedAt != null

        return if (isPaused) {
            when (kafkaStreams.state()) {
                KafkaStreams.State.CREATED -> UnHealthy(name, "Stream er pauset.")
                else -> UnHealthy(name, "Stream er pauset, men befinner seg i uventet state '${kafkaStreams.state().name}'.")
            }
        } else {
            if (kafkaStreams.state().isRunning) {
                Healthy(name, "Kjører som normalt.")
            } else UnHealthy(name, "Stream befinner seg i state '${kafkaStreams.state().name}'.")
        }
    }

    private val log = LoggerFactory.getLogger("no.nav.$name.stream")

    private companion object {
        private const val defaultConsiderRestartEveryInMinutes = 2L
        private const val closeTimeoutInMinutes = 2L
    }

    private val stateChangeLock = Semaphore(1)
    private var kafkaStreams = newKafkaStreams()
    private var restartConsiderer : Timer? = null
    private var pausedAt : LocalDateTime? = null

    init {
        start()
    }

    private fun start() {
        log.info("Starter")
        changeState {
            kafkaStreams.start()
            stopRestartConsiderer()
        }
    }

    internal fun stop() {
        log.info("Stopper")
        changeState {
            kafkaStreams.close(closeTimeoutInMinutes, TimeUnit.MINUTES)
            stopRestartConsiderer()
        }
    }

    private fun pause(throwable: Throwable) {
        log.info("Pauser")
        changeState {
            kafkaStreams.close(closeTimeoutInMinutes, TimeUnit.MINUTES)
            kafkaStreams = newKafkaStreams()
            startRestartConsiderer(throwable)
        }
    }

    private fun stopRestartConsiderer() {
        pausedAt = null
        restartConsiderer?.cancel()
        restartConsiderer = null
    }
    private fun startRestartConsiderer(throwable: Throwable) {
        pausedAt = LocalDateTime.now()
        restartConsiderer?.cancel()
        restartConsiderer = fixedRateTimer(
            name = "${name}_restartConsiderer",
            initialDelay = considerRestartEvery.toMillis(),
            period = considerRestartEvery.toMillis()
        ) {
            log.info("Vurderer ny restart.")
            if (considerRestart(pausedAt!!, throwable)) {
                log.info("Restarter.")
                start()
            }
            log.info("Avventer restart.")
        }

    }
    private fun changeState(block: () -> Unit) {
        try {
            stateChangeLock.acquire()
            block()
        } finally {
            stateChangeLock.release()
        }
    }
    private fun newKafkaStreams() = addShutdownHook(KafkaStreams(topology, properties))
    private fun handleThrowable(throwable: Throwable) {
        if (pauseOn(throwable)) {
            log.warn("Pauser stream: ${throwable.message}", throwable.cause)
            pause(throwable)
        } else {
            log.error("Stopper stream. Uhåndtert feil", throwable)
            stop()
        }
    }
    private fun addShutdownHook(streams: KafkaStreams) : KafkaStreams{
        streams.setStateListener { newState, oldState ->
            log.info("Stream endret state fra $oldState til $newState")

            if (newState == KafkaStreams.State.ERROR) {
                log.error("Stopper stream ettersom State er ERROR.")
                stop()
            }
        }
        streams.setUncaughtExceptionHandler{ _, throwable ->
            handleThrowable(throwable)
        }
        Thread.currentThread().setUncaughtExceptionHandler { _, throwable ->
            handleThrowable(throwable)
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            stop()
        })

        return streams
    }
}