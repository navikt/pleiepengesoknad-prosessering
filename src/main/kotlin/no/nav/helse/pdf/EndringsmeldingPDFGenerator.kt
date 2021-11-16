package no.nav.helse.pdf

import no.nav.helse.pdf.PDFGenerator.Companion.DATE_FORMATTER
import no.nav.helse.prosessering.v1.asynkron.endringsmelding.EndringsmeldingV1
import no.nav.helse.prosessering.v2.somTekst
import no.nav.helse.utils.somNorskDag
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstaker
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidInfo
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.TilsynPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.Tilsynsordning

class EndringsmeldingPDFGenerator : PDFGenerator<EndringsmeldingV1>() {

    override val templateNavn: String
        get() = "endringsmelding"


    override fun EndringsmeldingV1.tilMap(): Map<String, Any?> {
        val ytelse = k9Format.getYtelse<PleiepengerSyktBarn>()
        return mapOf(
            "søknadId" to k9Format.søknadId.id,
            "mottattDag" to k9Format.mottattDato.withZoneSameInstant(ZONE_ID).somNorskDag(),
            "mottattDato" to DATE_TIME_FORMATTER.format(k9Format.mottattDato),
            "soker" to mapOf(
                "navn" to søker.formatertNavn().capitalizeName(),
                "fødselsnummer" to søker.fødselsnummer
            ),
            "barn" to ytelse.barn.somMap(),
            "arbeidstid" to when {
                ytelse.arbeidstid != null -> ytelse.arbeidstid.somMap()
                else -> null
            },
            "omsorgstilbud" to when {
                ytelse.tilsynsordning != null && !ytelse.tilsynsordning.perioder.isNullOrEmpty() -> ytelse.tilsynsordning.somMap()
                else -> null
            },
            "samtykke" to mapOf(
                "har_forstatt_rettigheter_og_plikter" to harForståttRettigheterOgPlikter,
                "har_bekreftet_opplysninger" to harBekreftetOpplysninger
            )
        )
    }

    override val bilder: Map<String, String>
        get() = mapOf()

}

private fun Tilsynsordning.somMap(): Map<String, Any?> = mapOf(
    "perioder" to this.perioder.somMap()
)

@JvmName("somMapPeriodeTilsynPeriodeInfo")
private fun MutableMap<Periode, TilsynPeriodeInfo>.somMap(): List<Map<String, Any?>> = map { entry ->
    mapOf(
        "periode" to entry.key.somMap(),
        "tilsynPeriodeInfo" to entry.value.somMap()
    )
}

private fun TilsynPeriodeInfo.somMap(): Map<String, Any?> = mapOf(
    "etablertTilsynTimerPerDag" to etablertTilsynTimerPerDag.somTekst()
)

private fun Barn.somMap(): Map<String, Any?> = mapOf(
    "fødselsnummer" to personIdent.verdi
)

fun Arbeidstid.somMap(): Map<String, Any?> = mapOf(
    "arbeidstakerList" to when {
        !arbeidstakerList.isNullOrEmpty() -> arbeidstakerList.somMap()
        else -> null
    },
    "frilanserArbeidstidInfo" to when {
        frilanserArbeidstidInfo.isPresent -> frilanserArbeidstidInfo.get().somMap()
        else -> null
    },
    "selvstendigNæringsdrivendeArbeidstidInfo" to when {
        selvstendigNæringsdrivendeArbeidstidInfo.isPresent -> selvstendigNæringsdrivendeArbeidstidInfo.get().somMap()
        else -> null
    }
)

fun List<Arbeidstaker>.somMap(): List<Map<String, Any?>> = map { arbeidstaker ->
    mapOf(
        "organisasjonsnummer" to arbeidstaker.organisasjonsnummer.verdi,
        "arbeidstidInfo" to arbeidstaker.arbeidstidInfo.somMap()
    )
}

fun ArbeidstidInfo.somMap(): Map<String, Any?> = mapOf(
    "perioder" to perioder.somMap()
)

fun MutableMap<Periode, ArbeidstidPeriodeInfo>.somMap(): List<Map<String, Any?>> = map { entry ->
    mapOf(
        "periode" to entry.key.somMap(),
        "arbeidstidPeriodeInfo" to entry.value.somMap()
    )
}

fun Periode.somMap(): Map<String, Any?> = mutableMapOf(
    "fraOgMed" to DATE_FORMATTER.format(fraOgMed),
    "tilOgMed" to DATE_FORMATTER.format(tilOgMed)
)

fun ArbeidstidPeriodeInfo.somMap(): Map<String, Any?> = mutableMapOf(
    "jobberNormaltTimerPerDag" to jobberNormaltTimerPerDag.somTekst(),
    "faktiskArbeidTimerPerDag" to faktiskArbeidTimerPerDag.somTekst()
)