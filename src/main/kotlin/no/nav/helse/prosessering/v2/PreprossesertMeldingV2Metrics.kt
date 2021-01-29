package no.nav.helse.prosessering.v1

import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import no.nav.helse.prosessering.v2.PreprossesertMeldingV2
import no.nav.helse.utils.tilJaEllerNei
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

private val ZONE_ID = ZoneId.of("Europe/Oslo")

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


private val gradHistogram = Histogram.build()
    .buckets(20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0)
    .name("grad_histogram")
    .help("Graden søknaden gjelder")
    .register()

private val barnetsAlderHistogram = Histogram.build()
    .buckets(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0)
    .name("barnets_alder_histogram")
    .help("Alderen på barnet det søkes for")
    .register()

private val idTypePaaBarnCounter = Counter.build()
    .name("id_type_paa_barn_counter")
    .help("Teller for hva slags ID-Type som er benyttet for å identifisere barnet")
    .labelNames("id_type")
    .register()

private val jaNeiCounter = Counter.build()
    .name("ja_nei_counter")
    .help("Teller for svar på ja/nei spørsmål i søknaden")
    .labelNames("spm", "svar")
    .register()

private val barnetsAlderIUkerCounter = Counter.build()
    .name("barnets_alder_i_uker")
    .help("Teller for barn under 1 år, hvor mange uker de er.")
    .labelNames("uker")
    .register()

internal fun PreprossesertMeldingV2.reportMetrics() {
    val barnetsFodselsdato = barn.fodseldato() ?: barn.fødselsdato
    if (barnetsFodselsdato != null) {
        val barnetsAlder = barnetsFodselsdato.aarSiden()
        barnetsAlderHistogram.observe(barnetsAlder)
        if (barnetsAlder.erUnderEttAar()) {
            barnetsAlderIUkerCounter.labels(barnetsFodselsdato.ukerSiden()).inc()
        }
    }
    valgteArbeidsgivereHistogram.observe(arbeidsgivere.organisasjoner.size.toDouble())
    idTypePaaBarnCounter.labels(barn.idType()).inc()
    periodeSoknadGjelderIUkerHistogram.observe(ChronoUnit.WEEKS.between(fraOgMed, tilOgMed).toDouble())

    jaNeiCounter.labels("har_medsoker", harMedsøker.tilJaEllerNei()).inc()
    jaNeiCounter.labels("har_bodd_i_utlandet_siste_12_mnd", medlemskap.harBoddIUtlandetSiste12Mnd.tilJaEllerNei()).inc()
    jaNeiCounter.labels("skal_bo_i_utlandet_neste_12_mnd", medlemskap.skalBoIUtlandetNeste12Mnd.tilJaEllerNei()).inc()
}
