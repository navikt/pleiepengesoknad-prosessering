package no.nav.helse.prosessering.v1

import io.prometheus.client.Counter
import io.prometheus.client.Histogram

private val opplastedeVedleggHistogram = Histogram.build()
    .buckets(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
    .name("antall_oppplastede_vedlegg_histogram")
    .help("Antall vedlegg lastet opp i søknader")
    .register()

private val omsorgstilbudCounter = Counter.build()
    .name("omsorgstilbud_counter")
    .help("Teller for svar på ja på spørsmål om tilsynsordning i søknaden")
    .labelNames("spm", "svar")
    .register()

private val beredskapCounter = Counter.build()
    .name("beredskap_counter")
    .help("Teller for svar på ja på spørsmål om beredskap i søknaden")
    .labelNames("spm", "svar")
    .register()

private val nattevaakCounter = Counter.build()
    .name("nattevaak_counter")
    .help("Teller for svar på ja på spørsmål om nattevåk i søknaden")
    .labelNames("spm", "svar")
    .register()

internal fun MeldingV1.reportMetrics() {
    opplastedeVedleggHistogram.observe(vedleggUrls.size.toDouble())

    when (tilsynsordning?.svar) {
        "ja"  -> omsorgstilbudCounter.labels("omsorgstilbud", "ja").inc()
        "nei" -> omsorgstilbudCounter.labels("omsorgstilbud", "nei").inc()
        else -> omsorgstilbudCounter.labels("omsorgstilbud", "vetIkke").inc()
    }

    when (beredskap?.beredskap) {
        true  -> beredskapCounter.labels("beredskap", "ja").inc()
        false -> beredskapCounter.labels("beredskap", "nei").inc()
    }

    when (nattevåk?.harNattevåk) {
        true  -> nattevaakCounter.labels("nattevåk", "ja").inc()
        false -> nattevaakCounter.labels("nattevåk", "nei").inc()
    }
}