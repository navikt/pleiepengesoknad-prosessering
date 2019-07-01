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
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer

internal class PauseableKafkaStreams(
    private val name: String,
    private val topology: Topology,
    private val properties: Properties,
    private val considerRestartEvery : Duration = Duration.ofMinutes(1),
    private val considerRestart: (pausedAt: LocalDateTime, reason: Throwable) -> Boolean = { pausedAt, _ -> pausedAt.isBefore(LocalDateTime.now().minusMinutes(5)) }
) : HealthCheck {
    override suspend fun check(): Result {
        return when (state) {
            State.STOPPED -> UnHealthy(name, "Stream har stoppet")
            State.PAUSED -> UnHealthy(name, "Stream har pauset")
            else -> Healthy(name, "Stream er i state ${state.name}")
        }
    }
    private val log = LoggerFactory.getLogger(name)

    private companion object {
        private const val closeTimeoutInMinutes = 2L
    }

    private val stateChangeLock = Semaphore(1)
    private var state = State.INITIALIZED
    private var kafkaStreams = newKafkaStreams()

    private var restartConsiderer : Timer? = null
    private var pausedAt : LocalDateTime? = null



    init {
        start()
    }

    private fun start() {
        changeState {
            when (state) {
                State.INITIALIZED -> {
                    log.info("Starter stream for første gang.")
                    kafkaStreams.start()
                    log.info("Stream startet for første gang.")
                }
                State.STARTED -> log.info("Stream allerede startet.")
                State.PAUSED -> {
                    log.info("Starter stream igjen etter pause.")
                    stopRestartConsiderer()
                    kafkaStreams.start()
                    state = State.STARTED
                    log.info("Streams startet igjen etter pause.")
                }
                State.STOPPED -> throw IllegalStateException("Stream kan ikke gjenåpnes når det er stoppet.")
            }
        }
    }

    internal fun stop() {
        changeState {
            when (state) {
                State.STARTED, State.INITIALIZED -> {
                    log.info("Stopper stream.")
                    kafkaStreams.close(closeTimeoutInMinutes, TimeUnit.MINUTES)
                    state = State.STOPPED
                    log.info("Stream stoppet.")
                }
                else -> log.info("Trenger ikke stoppe stream som er i state ${state.name}.")
            }
        }
    }

    internal fun pause(cause: Throwable) {
        changeState {
            when (state) {
                State.PAUSED -> log.info("Stream er allerede pauset.")
                State.STARTED -> {
                    log.info("Pause stream.")
                    kafkaStreams.close(closeTimeoutInMinutes, TimeUnit.MINUTES)
                    kafkaStreams = newKafkaStreams()
                    pausedAt = LocalDateTime.now()
                    startRestartConsiderer(cause)
                    state = State.PAUSED
                    log.info("Stream pauset.")
                }
                else -> throw IllegalStateException("Stream kan ikke pauses når den er i state ${state.name}.")
            }
        }
    }

    private fun stopRestartConsiderer() {
        pausedAt = null
        restartConsiderer?.cancel()
        restartConsiderer = null
    }
    private fun startRestartConsiderer(throwable: Throwable) {
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
        if (throwable is PauseableError) {
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
            println("Stopping stream.")
            stop()
        })

        return streams
    }
}

private enum class State { INITIALIZED, STARTED, PAUSED, STOPPED }
class PauseableError(message: String, cause: Throwable?) : Throwable(message, cause)