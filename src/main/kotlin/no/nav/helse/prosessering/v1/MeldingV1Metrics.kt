package no.nav.helse.prosessering.v1

import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import java.time.temporal.ChronoUnit

private val periodeSoknadGjelderIUkerHistogram = Histogram.build()
    .buckets(0.00, 1.00, 4.00, 8.00, 12.00, 16.00, 20.00, 24.00, 28.00, 32.00, 36.00, 40.00, 44.00, 48.00, 52.00)
    .name("antall_uker_soknaden_gjelder_histogram")
    .help("Antall uker søknaden gjelder")
    .register()

private val valgteArbeidsgivereHistogram = Histogram.build()
    .buckets(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
    .name("antall_valgte_arbeidsgivere_histogram")
    .help("Antall arbeidsgivere valgt i søknadene")
    .register()

private val opplastedeVedleggHistogram = Histogram.build()
    .buckets(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
    .name("antall_oppplastede_vedlegg_histogram")
    .help("Antall vedlegg lastet opp i søknader")
    .register()

private val gradHistogram = Histogram.build()
    .buckets(20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0)
    .name("grad_histogram")
    .help("Graden søknaden gjelder")
    .register()

private val idTypePaaBarnCounter = Counter.build()
    .name("id_type_paa_barn_counter")
    .help("Teller for hva slags ID-Type som er benyttet for å identifisere barnet")
    .labelNames("id_type")
    .register()

internal fun MeldingV1.reportMetrics() {
    valgteArbeidsgivereHistogram.observe(arbeidsgivere.organisasjoner.size.toDouble())
    opplastedeVedleggHistogram.observe( (vedlegg.size + vedleggUrls.size).toDouble() )
    idTypePaaBarnCounter.labels(if (barn.fodselsnummer != null) "fodselsnummer" else "alternativ_id").inc()
    periodeSoknadGjelderIUkerHistogram.observe(ChronoUnit.WEEKS.between(fraOgMed, tilOgMed).toDouble())
    gradHistogram.observe(grad.toDouble())
}