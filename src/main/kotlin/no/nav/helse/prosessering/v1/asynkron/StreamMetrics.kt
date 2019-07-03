package no.nav.helse.prosessering.v1.asynkron

import io.prometheus.client.Counter

internal class StreamProcessingStatusCounter(private val name: String) {
    private companion object {
        private val counter = Counter.build()
            .name("stream_processing_status_counter")
            .help("Teller for status av prosessering av meldinger på streams.")
            .labelNames("stream", "status")
            .register()
    }

    internal fun ok() = counter.labels(name, "OK").inc()
    internal fun tryAgain() = counter.labels(name, "TRY_AGAIN").inc()
    internal fun exhausted() = counter.labels(name, "EXHAUSTED")
}