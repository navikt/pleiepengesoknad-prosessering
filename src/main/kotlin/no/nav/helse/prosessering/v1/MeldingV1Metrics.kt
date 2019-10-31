package no.nav.helse.prosessering.v1

import io.prometheus.client.Histogram

private val opplastedeVedleggHistogram = Histogram.build()
    .buckets(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
    .name("antall_oppplastede_vedlegg_histogram")
    .help("Antall vedlegg lastet opp i søknader")
    .register()

internal fun MeldingV1.reportMetrics() {
    opplastedeVedleggHistogram.observe(vedleggUrls.size.toDouble())
}