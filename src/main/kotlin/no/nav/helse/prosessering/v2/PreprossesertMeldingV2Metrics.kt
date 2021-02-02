package no.nav.helse.prosessering.v2

import no.nav.helse.prosessering.v1.barnetsAlderHistogram
import no.nav.helse.prosessering.v1.barnetsAlderIUkerCounter
import no.nav.helse.prosessering.v1.idTypePaaBarnCounter
import no.nav.helse.prosessering.v1.jaNeiCounter
import no.nav.helse.prosessering.v1.periodeSoknadGjelderIUkerHistogram
import no.nav.helse.prosessering.v1.valgteArbeidsgivereHistogram
import no.nav.helse.utils.*
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit.WEEKS

private val ZONE_ID = ZoneId.of("Europe/Oslo")


internal fun PreprossesertMeldingV2.reportMetrics() {
    val ytelse = søknad.getYtelse<PleiepengerSyktBarn>()
    val barnetsFodselsdato = ytelse.barn.fodseldato() ?: ytelse.barn.fødselsdato
    if (barnetsFodselsdato != null) {
        val barnetsAlder = barnetsFodselsdato.aarSiden()
        barnetsAlderHistogram.observe(barnetsAlder)
        if (barnetsAlder.erUnderEttAar()) {
            barnetsAlderIUkerCounter.labels(barnetsFodselsdato.ukerSiden()).inc()
        }
    }
    valgteArbeidsgivereHistogram.observe(ytelse.arbeidstid.arbeidstakerList.size.toDouble())
    idTypePaaBarnCounter.labels(ytelse.barn.idType()).inc()
    periodeSoknadGjelderIUkerHistogram.observe(
        WEEKS.between(ytelse.søknadsperiode.fraOgMed, ytelse.søknadsperiode.tilOgMed).toDouble()
    )

    jaNeiCounter.labels("har_medsoker", ytelse.søknadInfo.harMedsøker.tilJaEllerNei()).inc()
}

private fun Barn.fodseldato(): LocalDate? {
    if (norskIdentitetsnummer == null) return null
    return try {
        val dag = norskIdentitetsnummer.verdi.substring(0, 2).toInt()
        val maned = norskIdentitetsnummer.verdi.substring(2, 4).toInt()
        val ar = "20${norskIdentitetsnummer.verdi.substring(4, 6)}".toInt()
        LocalDate.of(ar, maned, dag)
    } catch (cause: Throwable) {
        null
    }
}

private fun Barn.idType(): String {
    return when {
        norskIdentitetsnummer != null -> "fodselsnummer"
        fødselsdato != null -> "fodselsdato"
        else -> "ingen_id"
    }
}
