package no.nav.helse.pdf

import com.fasterxml.jackson.core.type.TypeReference
import no.nav.helse.felles.*
import no.nav.helse.pdf.PDFGenerator.Companion.DATE_FORMATTER
import no.nav.helse.prosessering.v1.Arbeidsgiver
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.somTekst
import no.nav.helse.utils.DateUtils
import no.nav.helse.utils.somNorskDag
import no.nav.helse.utils.somNorskMåned
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

class SøknadPDFGenerator : PDFGenerator<MeldingV1>() {

    override val templateNavn: String
        get() = "soknad"


    override fun MeldingV1.tilMap(): Map<String, Any?> =  mapOf(
        "søknad" to somMap(),
        "soknad_id" to søknadId,
        "soknad_mottatt_dag" to mottatt.withZoneSameInstant(ZONE_ID).somNorskDag(),
        "soknad_mottatt" to DATE_TIME_FORMATTER.format(mottatt),
        "har_medsoker" to harMedsøker,
        "harIkkeVedlegg" to sjekkOmHarIkkeVedlegg(),
        "samtidig_hjemme" to samtidigHjemme,
        "soker" to mapOf(
            "navn" to søker.formatertNavn().capitalizeName(),
            "fodselsnummer" to søker.fødselsnummer
        ),
        "barn" to mapOf(
            "id" to barn.fødselsnummer,
            "navn" to barn.navn.capitalizeName(),
            "fødselsdato" to if(barn.fødselsdato != null) DATE_FORMATTER.format(barn.fødselsdato) else null,
            "årsakManglerIdentitetsnummer" to if(barn.årsakManglerIdentitetsnummer != null) barn.årsakManglerIdentitetsnummer.pdfTekst else null
        ),
        "periode" to mapOf(
            "fra_og_med" to DATE_FORMATTER.format(fraOgMed),
            "til_og_med" to DATE_FORMATTER.format(tilOgMed),
            "virkedager" to DateUtils.antallVirkedager(fraOgMed, tilOgMed)
        ),
        "medlemskap" to mapOf(
            "har_bodd_i_utlandet_siste_12_mnd" to medlemskap.harBoddIUtlandetSiste12Mnd,
            "utenlandsopphold_siste_12_mnd" to medlemskap.utenlandsoppholdSiste12Mnd.somMapBosted(),
            "skal_bo_i_utlandet_neste_12_mnd" to medlemskap.skalBoIUtlandetNeste12Mnd,
            "utenlandsopphold_neste_12_mnd" to medlemskap.utenlandsoppholdNeste12Mnd.somMapBosted()
        ),
        "samtykke" to mapOf(
            "har_forstatt_rettigheter_og_plikter" to harForståttRettigheterOgPlikter,
            "har_bekreftet_opplysninger" to harBekreftetOpplysninger
        ),
        "hjelp" to mapOf(
            "har_medsoker" to harMedsøker,
            "ingen_arbeidsgivere" to arbeidsgivere.isEmpty(),
            "sprak" to språk?.sprakTilTekst()
        ),
        "omsorgstilbud" to omsorgstilbud?.somMap(),
        "nattevaak" to nattevåk(nattevåk),
        "beredskap" to beredskap(beredskap),
        "utenlandsoppholdIPerioden" to mapOf(
            "skalOppholdeSegIUtlandetIPerioden" to utenlandsoppholdIPerioden.skalOppholdeSegIUtlandetIPerioden,
            "opphold" to utenlandsoppholdIPerioden.opphold.somMapUtenlandsopphold()
        ),
        "ferieuttakIPerioden" to mapOf(
            "skalTaUtFerieIPerioden" to ferieuttakIPerioden?.skalTaUtFerieIPerioden,
            "ferieuttak" to ferieuttakIPerioden?.ferieuttak?.somMapFerieuttak()
        ),
        "barnRelasjon" to barnRelasjon?.utskriftsvennlig,
        "barnRelasjonBeskrivelse" to barnRelasjonBeskrivelse,
        "harVærtEllerErVernepliktig" to harVærtEllerErVernepliktig,
        "frilans" to frilans.somMap(),
        "selvstendigNæringsdrivende" to selvstendigNæringsdrivende.somMap(),
        "arbeidsgivere" to arbeidsgivere.somMapAnsatt(),
        "hjelper" to mapOf(
            "harFlereAktiveVirksomheterErSatt" to harFlereAktiveVirksomehterSatt(),
            "harVærtEllerErVernepliktigErSatt" to erBooleanSatt(harVærtEllerErVernepliktig),
            "ingen_arbeidsforhold" to !harMinstEtArbeidsforhold()
        )
    )

    override val bilder: Map<String, String>
        get() = mapOf()

    private fun MeldingV1.somMap() = mapper.convertValue(
        this,
        object :
            TypeReference<MutableMap<String, Any?>>() {}
    )
}

private fun MeldingV1.harMinstEtArbeidsforhold(): Boolean {
    if (frilans.arbeidsforhold != null) return true

    if (selvstendigNæringsdrivende.arbeidsforhold != null) return true

    if (arbeidsgivere.any() { it.arbeidsforhold != null }) return true

    return false
}

private fun MeldingV1.harFlereAktiveVirksomehterSatt() =
    (this.selvstendigNæringsdrivende.virksomhet?.harFlereAktiveVirksomheter != null)

private fun erBooleanSatt(verdi: Boolean?) = verdi != null

private fun nattevåk(nattevaak: Nattevåk?) = when {
    nattevaak == null -> null
    else -> {
        mapOf(
            "har_nattevaak" to nattevaak.harNattevåk,
            "tilleggsinformasjon" to nattevaak.tilleggsinformasjon
        )
    }
}

private fun beredskap(beredskap: Beredskap?) = when {
    beredskap == null -> null
    else -> {
        mapOf(
            "i_beredskap" to beredskap.beredskap,
            "tilleggsinformasjon" to beredskap.tilleggsinformasjon
        )
    }
}

private fun Omsorgstilbud.somMap(): Map<String, Any?> {
    return mapOf(
        "erLiktHverUke" to erLiktHverUke,
        "enkeltdagerPerMnd" to enkeltdager?.somMapPerMnd(),
        "ukedager" to ukedager?.somMap()
    )
}

private fun List<Enkeltdag>.somMapEnkeltdag(): List<Map<String, Any?>> {
    return map {
        mapOf<String, Any?>(
            "dato" to DATE_FORMATTER.format(it.dato),
            "dag" to it.dato.dayOfWeek.somNorskDag(),
            "tid" to it.tid.somTekst(avkort = false)
        )
    }
}

fun List<Enkeltdag>.somMapPerMnd(): List<Map<String, Any>> {
    val omsorgsdagerPerMnd = this.groupBy { it.dato.month }

    return omsorgsdagerPerMnd.map {
        mapOf(
            "år" to it.value.first().dato.year,
            "måned" to it.key.somNorskMåned().capitalizeName(),
            "enkeltdagerPerUke" to it.value.somMapPerUke()
        )
    }
}

private fun List<Enkeltdag>.somMapPerUke(): List<Map<String, Any>> {
    val omsorgsdagerPerUke = this.groupBy {
        val uketall = it.dato.get(WeekFields.of(Locale.getDefault()).weekOfYear())
        if(uketall == 0) 53 else uketall
    }
    return omsorgsdagerPerUke.map {
        mapOf(
            "uke" to it.key,
            "dager" to it.value.somMapEnkeltdag()
        )
    }
}

private fun PlanUkedager.somMap(avkort: Boolean = true) = mapOf<String, Any?>(
    "mandag" to if (mandag.harGyldigVerdi()) mandag!!.somTekst(avkort) else null,
    "tirsdag" to if (tirsdag.harGyldigVerdi()) tirsdag!!.somTekst(avkort) else null,
    "onsdag" to if (onsdag.harGyldigVerdi()) onsdag!!.somTekst(avkort) else null,
    "torsdag" to if (torsdag.harGyldigVerdi()) torsdag!!.somTekst(avkort) else null,
    "fredag" to if (fredag.harGyldigVerdi()) fredag!!.somTekst(avkort) else null,
)

private fun Duration?.harGyldigVerdi() = this != null && this != Duration.ZERO

private fun Arbeidsforhold.somMap(): Map<String, Any?> = mapOf(
    "data" to this.toString(),
    "normalarbeidstid" to this.normalarbeidstid.somMap(),
    "arbeidIPeriode" to this.arbeidIPeriode.somMap()
)

private fun ArbeidIPeriode.somMap(): Map<String, Any?> = mapOf(
    "type" to this.type.name,
    "timerPerUke" to this.timerPerUke?.tilString(),
    "prosentAvNormalt" to this.prosentAvNormalt,
    "enkeltdager" to this.enkeltdager?.somMap(),
    "fasteDager" to this.fasteDager?.somMap(false)
)

private fun List<ArbeidstidEnkeltdag>.somMap() = map {
    mapOf(
        "dato" to DATE_FORMATTER.format(it.dato),
        "normalTimer" to it.arbeidstimer.normalTimer.tilString(),
        "faktiskTimer" to it.arbeidstimer.faktiskTimer.tilString()
    )
}
private fun NormalArbeidstid.somMap(): Map<String, Any?> = mapOf(
    "timerPerUkeISnitt" to this.timerPerUkeISnitt?.tilString(),
    "timerFasteDager" to this.timerFasteDager?.somMap(false)
)

private fun Frilans.somMap() : Map<String, Any?> = mapOf(
    "harInntektSomFrilanser" to harInntektSomFrilanser,
    "startdato" to if(startdato != null) DATE_FORMATTER.format(startdato) else null,
    "sluttdato" to if(sluttdato != null) DATE_FORMATTER.format(sluttdato) else null,
    "jobberFortsattSomFrilans" to jobberFortsattSomFrilans,
    "arbeidsforhold" to arbeidsforhold?.somMap()
)

private fun SelvstendigNæringsdrivende.somMap() : Map<String, Any?> = mapOf(
    "harInntektSomSelvstendig" to harInntektSomSelvstendig,
    "virksomhet" to virksomhet?.somMap(),
    "arbeidsforhold" to arbeidsforhold?.somMap()
)

private fun Virksomhet.somMap() : Map<String, Any?> = mapOf(
    "næringstyper" to næringstyper,
    "næringsinntekt" to næringsinntekt,
    "yrkesaktivSisteTreFerdigliknedeÅrene" to yrkesaktivSisteTreFerdigliknedeÅrene?.somMap(),
    "varigEndring" to varigEndring?.somMap(),
    "harFlereAktiveVirksomheter" to harFlereAktiveVirksomheter,
    "navnPåVirksomheten" to navnPåVirksomheten,
    "fraOgMed" to DATE_FORMATTER.format(fraOgMed),
    "tilOgMed" to if(tilOgMed != null) DATE_FORMATTER.format(tilOgMed) else null,
    "næringstyper" to næringstyper.somMapNæringstyper(),
    "fiskerErPåBladB" to fiskerErPåBladB,
    "registrertINorge" to registrertINorge,
    "organisasjonsnummer" to organisasjonsnummer,
    "registrertIUtlandet" to registrertIUtlandet?.somMap(),
    "regnskapsfører" to regnskapsfører?.somMap()
)

private fun List<Næringstyper>.somMapNæringstyper() = map {
    mapOf(
        "navn" to it.beskrivelse
    )
}

private fun Regnskapsfører.somMap() = mapOf<String, Any?>(
    "navn" to navn,
    "telefon" to telefon
)

private fun Land.somMap() = mapOf<String, Any?>(
    "landnavn" to landnavn,
    "landkode" to landkode
)

private fun YrkesaktivSisteTreFerdigliknedeÅrene.somMap() : Map<String, Any?> = mapOf(
    "oppstartsdato" to DATE_FORMATTER.format(oppstartsdato)
)
private fun VarigEndring.somMap() : Map<String, Any?> = mapOf(
    "dato" to DATE_FORMATTER.format(dato),
    "inntektEtterEndring" to inntektEtterEndring,
    "forklaring" to forklaring
)

private fun List<Arbeidsgiver>.somMapAnsatt() = map {
    mapOf(
        "navn" to it.navn,
        "organisasjonsnummer" to it.organisasjonsnummer,
        "erAnsatt" to it.erAnsatt,
        "arbeidsforhold" to it.arbeidsforhold?.somMap(),
        "sluttetFørSøknadsperiodeErSatt" to (it.sluttetFørSøknadsperiode != null),
        "sluttetFørSøknadsperiode" to it.sluttetFørSøknadsperiode
    )
}

private fun List<Bosted>.somMapBosted(): List<Map<String, Any?>> {
    return map {
        mapOf<String, Any?>(
            "landnavn" to it.landnavn,
            "fraOgMed" to DATE_FORMATTER.format(it.fraOgMed),
            "tilOgMed" to DATE_FORMATTER.format(it.tilOgMed)
        )
    }
}

private fun List<Utenlandsopphold>.somMapUtenlandsopphold(): List<Map<String, Any?>> {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.of("Europe/Oslo"))
    return map {
        mapOf(
            "landnavn" to it.landnavn,
            "landkode" to it.landkode,
            "fraOgMed" to dateFormatter.format(it.fraOgMed),
            "tilOgMed" to dateFormatter.format(it.tilOgMed),
            "erUtenforEØS" to it.erUtenforEøs,
            "erBarnetInnlagt" to it.erBarnetInnlagt,
            "perioderBarnetErInnlagt" to it.perioderBarnetErInnlagt.somMapPerioder(),
            "årsak" to it.årsak?.beskrivelse
        )
    }
}

private fun List<Ferieuttak>.somMapFerieuttak(): List<Map<String, Any?>> {
    return map {
        mapOf<String, Any?>(
            "fraOgMed" to DATE_FORMATTER.format(it.fraOgMed),
            "tilOgMed" to DATE_FORMATTER.format(it.tilOgMed)
        )
    }
}

private fun List<Periode>.somMapPerioder(): List<Map<String, Any?>> {
    return map {
        mapOf<String, Any?>(
            "fraOgMed" to DATE_FORMATTER.format(it.fraOgMed),
            "tilOgMed" to DATE_FORMATTER.format(it.tilOgMed)
        )
    }
}

fun Duration.tilString(): String = when (this.toMinutesPart()) {
    0 -> "${this.timer()} timer"
    else -> "${this.timer()} timer og ${this.toMinutesPart()} minutter"
}

fun Duration.timer() = (this.toDaysPart()*24) + this.toHoursPart()

fun Søker.formatertNavn() = if (mellomnavn != null) "$fornavn $mellomnavn $etternavn" else "$fornavn $etternavn"

fun String.capitalizeName(): String = split(" ").joinToString(" ") { name: String ->
    name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private fun String.sprakTilTekst() = when (this.lowercase()) {
    "nb" -> "bokmål"
    "nn" -> "nynorsk"
    else -> this
}

private fun MeldingV1.sjekkOmHarIkkeVedlegg(): Boolean = vedleggId.isEmpty()