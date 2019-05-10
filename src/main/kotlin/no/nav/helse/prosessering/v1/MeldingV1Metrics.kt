package no.nav.helse.prosessering.v1

import io.prometheus.client.Counter
import io.prometheus.client.Histogram
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

internal fun MeldingV1.reportMetrics() {
    val barnetsAlder = barn.metricAlder()
    if (barnetsAlder != null) barnetsAlderHistogram.observe(barnetsAlder)
    valgteArbeidsgivereHistogram.observe(arbeidsgivere.organisasjoner.size.toDouble())
    opplastedeVedleggHistogram.observe( (vedlegg.size + vedleggUrls.size).toDouble() )
    idTypePaaBarnCounter.labels(barn.idType()).inc()
    periodeSoknadGjelderIUkerHistogram.observe(ChronoUnit.WEEKS.between(fraOgMed, tilOgMed).toDouble())
    gradHistogram.observe(grad.toDouble())
    jaNeiCounter.labels("har_medsoker", harMedsoker.tilJaEllerNei()).inc()
    jaNeiCounter.labels("har_bodd_i_utlandet_siste_12_mnd", medlemskap.harBoddIUtlandetSiste12Mnd.tilJaEllerNei()).inc()
    jaNeiCounter.labels("skal_bo_i_utlandet_neste_12_mnd", medlemskap.skalBoIUtlandetNeste12Mnd.tilJaEllerNei()).inc()
}

private fun Barn.idType(): String {
    return when {
        fodselsnummer != null -> "fodselsnummer"
        alternativId != null -> "alternativ_id"
        else -> "ingen_id"
    }
}

internal fun Barn.metricAlder() : Double? {
    if (fodselsnummer == null) return null
    return try {
        val dag = fodselsnummer.substring(0,2).toInt()
        val maned = fodselsnummer.substring(2,4).toInt()
        val ar = "20${fodselsnummer.substring(4,6)}".toInt()
        val fodselsdato = LocalDate.of(ar, maned, dag)
        val alder= ChronoUnit.YEARS.between(fodselsdato, LocalDate.now(ZONE_ID))
        if (alder in -18..-1) return 19.0
        return alder.absoluteValue.toDouble()
    } catch (cause: Throwable) {
        null
    }
}

private fun Boolean.tilJaEllerNei(): String = if (this) "Ja" else "Nei"