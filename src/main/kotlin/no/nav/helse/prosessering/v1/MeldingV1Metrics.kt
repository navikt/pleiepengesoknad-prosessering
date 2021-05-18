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

private val frilansCounter = Counter.build()
    .name("frilansCounter")
    .help("Teller for frilans")
    .register()

private val arbeidstakerCounter = Counter.build()
    .name("arbeidstakerCounter")
    .help("Teller for arbeidstaker")
    .register()

private val selvstendigNæringsdrivendeOgFrilans = Counter.build()
    .name("selvstendigNaringsdrivendeOgFrilans")
    .help("Teller for selvstending næringsdrivende og frilans")
    .register()

private val selvstendigNæringsdrivendeFrilansOgArbeidstaker = Counter.build()
    .name("selvstendigNaringsdrivendeFrilansOgArbeidstaker")
    .help("Teller for selvstending næringsdrivende, frilans og arbeidstaker")
    .register()

private val selvstendingNæringsdrivendeOgArbeidstaker = Counter.build()
    .name("selvstendigNaringsdrivendeOgArbeidstaker")
    .help("Teller for selvstending næringsdrivende og arbeidstaker")
    .register()

private val frilansOgArbeidstaker = Counter.build()
    .name("frilansOgArbeidstaker")
    .help("Teller for frilans og arbeidstaker")
    .register()

private val selvstendigVirksomhetCounter = Counter.build()
    .name("selvstendigNaringsdrivendeCounter")
    .help("Teller for selvstending næringsdrivende")
    .register()

private val arbeidsgivereCounter = Counter.build()
    .name("arbeidsgivereCounter")
    .help("Teller for arbeidsgivere")
    .labelNames("antallArbeidsgivere", "skalJobbe")
    .register()

private val ingenInntektCounter = Counter.build()
    .name("ingenInntektCounter")
    .help("Teller for valg av verken arbeidsgiver, frilanser, eller selvstendig næringsdrivende")
    .labelNames()
    .register()

internal fun MeldingV1.reportMetrics() {
    opplastedeVedleggHistogram.observe(vedleggUrls.size.toDouble())

    when (omsorgstilbud) {
        null -> omsorgstilbudCounter.labels("omsorgstilbud", "nei").inc()
        else -> {
            when(omsorgstilbud.vetOmsorgstilbud) {
                VetOmsorgstilbud.VET_ALLE_TIMER -> omsorgstilbudCounter.labels("omsorgstilbud", "vetAlleTimer").inc()
                VetOmsorgstilbud.VET_NOEN_TIMER -> omsorgstilbudCounter.labels("omsorgstilbud", "vetNoenTimer").inc()
                VetOmsorgstilbud.VET_IKKE -> omsorgstilbudCounter.labels("omsorgstilbud", "vetIkke").inc()
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

    val skalJobbeString = arbeidsgivere.organisasjoner.map { it.skalJobbe.name.toLowerCase() }.sorted().joinToString("|")
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
