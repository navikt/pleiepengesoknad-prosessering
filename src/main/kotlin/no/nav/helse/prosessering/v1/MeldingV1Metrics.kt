package no.nav.helse.prosessering.v1

import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import java.time.LocalDate

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
val omsorgstilbudPlanEllerDagForDagCounter = Counter.build()
    .name("omsorgstilbud_plan_eller_dag_for_dag_counter")
    .help("Teller de som søker 6mnd frem i tid og har omsorgstilbud")
    .labelNames("type")
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

val jobbIPeriodenCounter = Counter.build()
    .name("jobbIPeriodenCounter")
    .help("Teller for om søker har jobbet i perioden")
    .labelNames("periode", "svar")
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

fun LocalDate.erFørDagensDato() = isBefore(LocalDate.now())
fun LocalDate.erLikDagensDato() = isEqual(LocalDate.now())
fun LocalDate.erEtterDagensDato() = isAfter(LocalDate.now())

internal fun MeldingV1.reportMetrics() {
    opplastedeVedleggHistogram.observe(vedleggId.size.toDouble())

    if (fraOgMed.erFørDagensDato() && (tilOgMed.erFørDagensDato() || tilOgMed.erLikDagensDato())) {
        søknadsperiodeCounter.labels("fortid").inc()
    }

    if (fraOgMed.erFørDagensDato() && (tilOgMed.erLikDagensDato() || tilOgMed.erEtterDagensDato())) {
        søknadsperiodeCounter.labels("fortidTilFremtid").inc()
    }

    if ((fraOgMed.erLikDagensDato() || fraOgMed.erEtterDagensDato()) && (tilOgMed.erLikDagensDato() || tilOgMed.erEtterDagensDato())) {
        søknadsperiodeCounter.labels("fremtid").inc()
    }

    when (omsorgstilbud) {
        null -> omsorgstilbudCounter.labels("omsorgstilbud", "nei").inc()
        else -> {
            if (omsorgstilbud.enkeltdager != null){
                omsorgstilbudPlanEllerDagForDagCounter.labels("enkeltdager").inc()
            } else if(omsorgstilbud.ukedager != null){
                omsorgstilbudPlanEllerDagForDagCounter.labels("ukedager").inc()
            }
        }
    }

    when (beredskap?.beredskap) {
        true -> beredskapCounter.labels("beredskap", "ja").inc()
        false -> beredskapCounter.labels("beredskap", "nei").inc()
        else -> {}
    }

    when (nattevåk?.harNattevåk) {
        true -> nattevaakCounter.labels("nattevåk", "ja").inc()
        false -> nattevaakCounter.labels("nattevåk", "nei").inc()
        else -> {}
    }
    // TODO: 04/02/2022 Metrikker for arbeid i perioden. Fast eller ukedager etc

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

private fun MeldingV1.harArbeidsforhold() = (this.arbeidsgivere != null && this.arbeidsgivere.isNotEmpty())

private fun MeldingV1.erArbeidstaker() =
    this.harArbeidsforhold() && selvstendigNæringsdrivende == null && frilans == null

private fun MeldingV1.erArbeidstakerOgFrilanser() =
    this.harArbeidsforhold() && selvstendigNæringsdrivende == null  && frilans != null

private fun MeldingV1.erArbeidstakerOgSelvstendigNæringsdrivende() =
    this.harArbeidsforhold() && selvstendigNæringsdrivende != null  && frilans == null

private fun MeldingV1.erArbeidstakerFrilanserOgSelvstendigNæringsdrivende() =
    this.harArbeidsforhold() && selvstendigNæringsdrivende != null && frilans != null

private fun MeldingV1.erFrilanserOgSelvstendigNæringsdrivende() =
    !this.harArbeidsforhold() && selvstendigNæringsdrivende != null && frilans != null

private fun MeldingV1.erFrilanser() =
    !this.harArbeidsforhold() && selvstendigNæringsdrivende == null && frilans != null

private fun MeldingV1.erSelvstendigNæringsdrivende() =
    !this.harArbeidsforhold() && selvstendigNæringsdrivende != null && frilans == null
