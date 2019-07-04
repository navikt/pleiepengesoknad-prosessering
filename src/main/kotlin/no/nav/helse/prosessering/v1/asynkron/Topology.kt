package no.nav.helse.prosessering.v1.asynkron

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.helse.dusseldorf.ktor.core.Retry
import java.time.Duration

internal fun <BEFORE, AFTER>process(
    name: String,
    soknadId: String,
    entry: TopicEntry<BEFORE>,
    block: suspend() -> AFTER) : TopicEntry<AFTER> {
    return runBlocking(MDCContext(mapOf(
        "correlation_id" to entry.metadata.correlationId,
        "request_id" to entry.metadata.requestId,
        "soknad_id" to soknadId
    ))) {
        Retry.retry(
            operation = name,
            initialDelay = Duration.ofSeconds(5),
            maxDelay = Duration.ofSeconds(10)
        ) {
            TopicEntry(
                metadata = entry.metadata,
                data = block()
            )
        }
    }
}