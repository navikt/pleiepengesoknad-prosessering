package no.nav.helse.kafka

import io.prometheus.client.Counter
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.prosessering.Metadata
import java.time.Duration

data class TopicEntry<V>(val metadata: Metadata, val data: V)

private object StreamCounter {
    private val counter = Counter.build()
        .name("stream_processing_status_counter")
        .help("Teller for status av prosessering av meldinger på streams.")
        .labelNames("stream", "status")
        .register()
    internal fun ok(name: String) = counter.labels(name, "OK").inc()
    internal fun feil(name: String) = counter.labels(name, "FEIL").inc()
}

internal fun <BEFORE, AFTER>process(
    name: String,
    soknadId: String,
    entry: TopicEntry<BEFORE>,
    block: suspend() -> AFTER) : TopicEntry<AFTER> {
    return runBlocking(MDCContext(mapOf(
        "correlation_id" to entry.metadata.correlationId,
        "soknad_id" to soknadId
    ))) {
        val processed = try {
            Retry.retry(
                operation = name,
                initialDelay = Duration.ofSeconds(5),
                maxDelay = Duration.ofSeconds(10)
            ) { block() }
        } catch (cause: Throwable) {
            StreamCounter.feil(name)
            throw cause
        }
        StreamCounter.ok(name)
        TopicEntry(
            metadata = entry.metadata,
            data = processed
        )
    }
}
