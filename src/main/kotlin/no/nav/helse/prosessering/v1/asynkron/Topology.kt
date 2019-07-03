package no.nav.helse.prosessering.v1.asynkron

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.time.delay
import no.nav.helse.prosessering.Metadata
import org.slf4j.Logger
import org.slf4j.MDC
import java.time.Duration

internal fun <BEFORE, AFTER>process(
    soknadId: String,
    entry: TopicEntry<BEFORE>,
    logger: Logger,
    maxAttempts: Int = 3,
    block: suspend() -> AFTER) : Result<BEFORE, AFTER> {
    MDC.put("correlation_id", entry.metadata.correlationId)
    MDC.put("request_id", entry.metadata.requestId)
    MDC.put("attempt", "${entry.metadata.attempt}")
    MDC.put("soknad_id", soknadId)
    return runBlocking(MDCContext()) {
        try {
            Result(
                maxAttempts,
                entry,
                TopicEntry(
                    metadata = entry.metadata,
                    data = block()
                )
            )
        } catch (cause: Throwable) {
            logger.warn("Uventet feil oppsto ved prosessering av melding", cause)
            Result<BEFORE, AFTER>(maxAttempts, entry)
        }
    }
}

internal fun <BEFORE>peekAttempts(
    soknadId: String,
    entry: TopicEntry<BEFORE>,
    logger: Logger
) = process(soknadId, entry, logger, 1) {
    val currentAttempt = entry.metadata.attempt
    if (currentAttempt > 1) {
        logger.info("Venter $currentAttempt sekunder før prosessering.")
        delay(Duration.ofSeconds(currentAttempt.toLong()))
    }
}

internal class Result<BEFORE, AFTER>{
    private val maxAttempts : Int
    private val before : TopicEntry<BEFORE>
    private val after : TopicEntry<AFTER>?

    constructor(maxAttempts: Int, before: TopicEntry<BEFORE>) {
        this.maxAttempts = maxAttempts
        this.before = before
        this.after = null
    }
    constructor(maxAttempts: Int, before: TopicEntry<BEFORE>, after: TopicEntry<AFTER>) {
        this.maxAttempts = maxAttempts
        this.before = before
        this.after = after
    }
    internal fun ok() = after != null
    internal fun exhausted() = before.metadata.attempt > maxAttempts
    internal fun tryAgain() = before.metadata.attempt <= maxAttempts

    internal fun before() = TopicEntry(
        metadata = Metadata(before.metadata),
        data = before.data
    )
    internal fun after() = after!!
}

