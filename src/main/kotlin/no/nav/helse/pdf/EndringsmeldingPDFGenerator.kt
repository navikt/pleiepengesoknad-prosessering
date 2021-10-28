package no.nav.helse.pdf

import com.fasterxml.jackson.core.type.TypeReference
import no.nav.helse.pdf.PDFGenerator.Companion.DATE_FORMATTER
import no.nav.helse.prosessering.v1.asynkron.endringsmelding.EndringsmeldingV1
import no.nav.helse.prosessering.v2.somTekst
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstaker
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidInfo
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidPeriodeInfo

class EndringsmeldingPDFGenerator : PDFGenerator<EndringsmeldingV1>() {

    override val templateNavn: String
        get() = "endringsmelding"


    override fun EndringsmeldingV1.tilMap(): Map<String, Any?> = mapOf(
        "endringsmelding" to somMap(),
        "soker" to mapOf(
            "navn" to søker.formatertNavn().capitalizeName(),
            "fodselsnummer" to søker.fødselsnummer
        ),
        "arbeidstid" to k9Format.getYtelse<PleiepengerSyktBarn>().arbeidstid.somMap(),
        "samtykke" to mapOf(
            "har_forstatt_rettigheter_og_plikter" to true,
            "har_bekreftet_opplysninger" to true
        )
    )

    override val bilder: Map<String, String>
        get() = mapOf()

    private fun EndringsmeldingV1.somMap() = mapper.convertValue(
        this,
        object :
            TypeReference<MutableMap<String, Any?>>() {}
    )
}

fun Arbeidstid.somMap(): Map<String, Any?> = mapOf(
    "arbeidstakerList" to arbeidstakerList.somMap(),
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

