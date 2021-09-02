package no.nav.helse.prosessering.v1

import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import no.nav.helse.felles.VetOmsorgstilbud
import java.time.LocalDate
import java.time.temporal.ChronoUnit

val opplastedeVedleggHistogram = Histogram.build()
    .buckets(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
    .name("antall_oppplastede_vedlegg_histogram")
    .help("Antall vedlegg lastet opp i søknader")
    .register()

val omsorgstilbudCounter = Counter.build()
    .name("omsorgstilbud_counter")
    .help("Teller for svar på ja på spørsmål om tilsynsordning i søknaden")
    .labelNames("spm", "svar")
    .register()

val beredskapCounter = Counter.build()
    .name("beredskap_counter")
    .help("Teller for svar på ja på spørsmål om beredskap i søknaden")
    .labelNames("spm", "svar")
    .register()

val nattevaakCounter = Counter.build()
    .name("nattevaak_counter")
    .help("Teller for svar på ja på spørsmål om nattevåk i søknaden")
    .labelNames("spm", "svar")
    .register()

val frilansCounter = Counter.build()
    .name("frilansCounter")
    .help("Teller for frilans")
    .register()

val arbeidstakerCounter = Counter.build()
    .name("arbeidstakerCounter")
    .help("Teller for arbeidstaker")
    .register()

val selvstendigNæringsdrivendeOgFrilans = Counter.build()
    .name("selvstendigNaringsdrivendeOgFrilans")
    .help("Teller for selvstending næringsdrivende og frilans")
    .register()

val selvstendigNæringsdrivendeFrilansOgArbeidstaker = Counter.build()
    .name("selvstendigNaringsdrivendeFrilansOgArbeidstaker")
    .help("Teller for selvstending næringsdrivende, frilans og arbeidstaker")
    .register()

val selvstendingNæringsdrivendeOgArbeidstaker = Counter.build()
    .name("selvstendigNaringsdrivendeOgArbeidstaker")
    .help("Teller for selvstending næringsdrivende og arbeidstaker")
    .register()

val frilansOgArbeidstaker = Counter.build()
    .name("frilansOgArbeidstaker")
    .help("Teller for frilans og arbeidstaker")
    .register()

val selvstendigVirksomhetCounter = Counter.build()
    .name("selvstendigNaringsdrivendeCounter")
    .help("Teller for selvstending næringsdrivende")
    .register()

val arbeidsgivereCounter = Counter.build()
    .name("arbeidsgivereCounter")
    .help("Teller for arbeidsgivere")
    .labelNames("antallArbeidsgivere", "skalJobbe")
    .register()

val ingenInntektCounter = Counter.build()
    .name("ingenInntektCounter")
    .help("Teller for valg av verken arbeidsgiver, frilanser, eller selvstendig næringsdrivende")
    .labelNames()
    .register()

val søknadsperiodeCounter = Counter.build()
    .name("soknadsperiodeCounter")
    .help("Teller for søknadsperiode.")
    .labelNames("tidsrom")
    .register()

val søknadsperiodeLengdeHistogram = Histogram.build()
    .buckets(
        0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0,
        10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0,
        20.0, 21.0, 22.0, 23.0, 24.0, 25.0, 26.0, 27.0, 28.0, 29.0,
        30.0, 31.0, 32.0, 33.0, 34.0, 35.0, 36.0, 37.0, 38.0, 39.0,
        40.0, 41.0, 42.0, 43.0, 44.0, 45.0, 46.0, 47.0, 48.0, 49.0,
        50.0, 51.0, 52.0
    )
    .name("soknadsperiodelengdeIUker")
    .help("Lengde på søknadsperiode i uker.")
    .register()

fun LocalDate.erFørDagensDato() = isBefore(LocalDate.now())
fun LocalDate.erLikDagensDato() = isEqual(LocalDate.now())
fun LocalDate.erEtterDagensDato() = isAfter(LocalDate.now())

internal fun MeldingV1.reportMetrics() {
    opplastedeVedleggHistogram.observe(vedleggUrls.size.toDouble())

    if (fraOgMed.erFørDagensDato() && (tilOgMed.erFørDagensDato() || tilOgMed.erLikDagensDato())) {
        søknadsperiodeCounter.labels("fortid").inc()
    }

    if (fraOgMed.erFørDagensDato() && (tilOgMed.erLikDagensDato() || tilOgMed.erEtterDagensDato())) {
        søknadsperiodeCounter.labels("fortidTilFremtid").inc()
    }

    if (fraOgMed.erLikDagensDato() && (tilOgMed.erLikDagensDato() || tilOgMed.erEtterDagensDato())) {
        søknadsperiodeCounter.labels("fremtid").inc()
    }

    søknadsperiodeLengdeHistogram.observe(ChronoUnit.WEEKS.between(fraOgMed, tilOgMed).toDouble())

    when (omsorgstilbudV2) {
        null -> omsorgstilbudCounter.labels("omsorgstilbud", "nei").inc()
        else -> {
            omsorgstilbudV2.apply {
                historisk?.let {
                    if (it.enkeltdager.isNotEmpty()) {
                        omsorgstilbudCounter.labels("omsorgstilbud", "historiskeEnkeltdager").inc()
                    }
                }

                planlagt?.let {
                    if (!it.enkeltdager.isNullOrEmpty()) {
                        omsorgstilbudCounter.labels("omsorgstilbud", "planlagteEnkeltdager").inc()
                    }

                    if (it.erLiktHverDag !== null && it.erLiktHverDag) {
                        omsorgstilbudCounter.labels("omsorgstilbud", "planlagteUkedagerErLiktHverDag").inc()
                    }

                    when (it.vetOmsorgstilbud) {
                        VetOmsorgstilbud.VET_ALLE_TIMER -> omsorgstilbudCounter.labels("omsorgstilbud", "vetAlleTimer")
                            .inc()
                        VetOmsorgstilbud.VET_IKKE -> omsorgstilbudCounter.labels("omsorgstilbud", "vetIkke").inc()
                    }
                }
            }
        }
    }

    when (beredskap?.beredskap) {
        true -> beredskapCounter.labels("beredskap", "ja").inc()
        false -> beredskapCounter.labels("beredskap", "nei").inc()
    }

    when (nattevåk?.harNattevåk) {
        true -> nattevaakCounter.labels("nattevåk", "ja").inc()
        false -> nattevaakCounter.labels("nattevåk", "nei").inc()
    }

    val skalJobbeString = arbeidsgivere.organisasjoner.map { it.skalJobbe.name.lowercase() }.sorted().joinToString("|")
    arbeidsgivereCounter.labels(arbeidsgivere.organisasjoner.size.toString(), skalJobbeString).inc()

    when {
        erArbeidstaker() -> arbeidstakerCounter.inc()
        erArbeidstakerOgFrilanser() -> frilansOgArbeidstaker.inc()
        erArbeidstakerOgSelvstendigNæringsdrivende() -> selvstendingNæringsdrivendeOgArbeidstaker.inc()
        erArbeidstakerFrilanserOgSelvstendigNæringsdrivende() -> selvstendigNæringsdrivendeFrilansOgArbeidstaker.inc()
        erFrilanserOgSelvstendigNæringsdrivende() -> selvstendigNæringsdrivendeOgFrilans.inc()
        erFrilanser() -> frilansCounter.inc()
        erSelvstendigNæringsdrivende() -> selvstendigVirksomhetCounter.inc()
        else -> ingenInntektCounter.inc()
    }
}

private fun MeldingV1.erArbeidstaker() =
    this.arbeidsgivere.organisasjoner.isNotEmpty() && selvstendigVirksomheter.isNullOrEmpty() && frilans == null

private fun MeldingV1.erArbeidstakerOgFrilanser() =
    this.arbeidsgivere.organisasjoner.isNotEmpty() && selvstendigVirksomheter.isNullOrEmpty() && frilans != null

private fun MeldingV1.erArbeidstakerOgSelvstendigNæringsdrivende() =
    this.arbeidsgivere.organisasjoner.isNotEmpty() && !selvstendigVirksomheter.isNullOrEmpty() && frilans == null

private fun MeldingV1.erArbeidstakerFrilanserOgSelvstendigNæringsdrivende() =
    this.arbeidsgivere.organisasjoner.isNotEmpty() && !selvstendigVirksomheter.isNullOrEmpty() && frilans != null

private fun MeldingV1.erFrilanserOgSelvstendigNæringsdrivende() =
    this.arbeidsgivere.organisasjoner.isEmpty() && !selvstendigVirksomheter.isNullOrEmpty() && frilans != null

private fun MeldingV1.erFrilanser() =
    this.arbeidsgivere.organisasjoner.isEmpty() && selvstendigVirksomheter.isNullOrEmpty() && frilans != null

private fun MeldingV1.erSelvstendigNæringsdrivende() =
    this.arbeidsgivere.organisasjoner.isEmpty() && !selvstendigVirksomheter.isNullOrEmpty() && frilans == null
