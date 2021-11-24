package no.nav.helse.pdf

import com.fasterxml.jackson.core.type.TypeReference
import no.nav.helse.felles.*
import no.nav.helse.pdf.PDFGenerator.Companion.DATE_FORMATTER
import no.nav.helse.prosessering.v1.*
import no.nav.helse.utils.DateUtils
import no.nav.helse.utils.somNorskDag
import no.nav.helse.utils.somNorskMåned
import java.time.Duration
import java.time.LocalDate
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
            "navn" to barn.navn.capitalizeName()
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
            "ingen_arbeidsgivere" to (arbeidsgivere == null),
            "sprak" to språk?.sprakTilTekst()
        ),
        "omsorgstilbud" to omsorgstilbudSomMap(fraOgMed, tilOgMed),
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
        "frilans" to frilans?.somMap(),
        "selvstendigNæringsdrivende" to selvstendigNæringsdrivende?.somMap(),
        "arbeidsgivere" to arbeidsgivere?.somMapAnsatt(),
        "hjelper" to mapOf( // TODO: 04/06/2021 Kan fjerne hjelpemetoden når feltet er prodsatt i api og front
            "harFlereAktiveVirksomheterErSatt" to harFlereAktiveVirksomehterSatt(),
            "harVærtEllerErVernepliktigErSatt" to erBooleanSatt(harVærtEllerErVernepliktig)
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

private fun MeldingV1.harFlereAktiveVirksomehterSatt() =
    (this.selvstendigNæringsdrivende?.virksomhet?.harFlereAktiveVirksomheter != null)

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

private fun MeldingV1.omsorgstilbudSomMap(fraOgMed: LocalDate, tilOgMed: LocalDate): Map<String, Any?> {
    val DAGENS_DATO = LocalDate.now()
    val GÅRSDAGENS_DATO = DAGENS_DATO.minusDays(1)

    return mapOf(
        "søknadsperiodeFraOgMed" to DATE_FORMATTER.format(fraOgMed),
        "søknadsperiodeTilOgMed" to DATE_FORMATTER.format(tilOgMed),
        "periodenStarterIFortid" to (fraOgMed.isBefore(DAGENS_DATO)),
        "fortidTilOgMed" to if(tilOgMed.isBefore(DAGENS_DATO)) DATE_FORMATTER.format(tilOgMed) else DATE_FORMATTER.format(GÅRSDAGENS_DATO),
        "periodenAvsluttesIFremtiden" to (tilOgMed.isAfter(GÅRSDAGENS_DATO)),
        "fremtidFraOgMed" to if(fraOgMed.isAfter(DAGENS_DATO)) DATE_FORMATTER.format(fraOgMed) else DATE_FORMATTER.format(DAGENS_DATO),
        "historisk" to omsorgstilbud?.historisk?.somMap(),
        "planlagt" to omsorgstilbud?.planlagt?.somMap(),
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
            "måned" to it.key.somNorskMåned().lowercase(),
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

private fun HistoriskOmsorgstilbud.somMap(): Map<String, Any?> = mutableMapOf(
    "enkeltdagerPerMnd" to enkeltdager.somMapPerMnd()
)

private fun PlanlagtOmsorgstilbud.somMap(): Map<String, Any?> = mutableMapOf(
    "enkeltdagerPerMnd" to enkeltdager?.somMapPerMnd(),
    "ukedager" to ukedager?.somMap(),
    "erLiktHverDag" to  erLiktHverDag,
    "harSvartPåErLiktHverDag" to  (erLiktHverDag != null)
)

private fun PlanUkedager.somMap() = mapOf<String, Any?>(
    "mandag" to (mandag?.somTekst() ?: "0 timer"),
    "tirsdag" to (tirsdag?.somTekst() ?: "0 timer"),
    "onsdag" to (onsdag?.somTekst() ?: "0 timer"),
    "torsdag" to (torsdag?.somTekst() ?: "0 timer"),
    "fredag" to (fredag?.somTekst() ?: "0 timer")
)

private fun Arbeidsforhold.somMap(
    skalViseHistoriskArbeid: Boolean = true,
    skalVisePlanlagtArbeid: Boolean = true
): Map<String, Any?> = mapOf(
    "jobberNormaltTimer" to jobberNormaltTimer,
    "historiskArbeid" to historiskArbeid?.somMap(),
    "planlagtArbeid" to planlagtArbeid?.somMap(),
    "skalViseHistoriskArbeid" to skalViseHistoriskArbeid,
    "skalVisePlanlagtArbeid" to skalVisePlanlagtArbeid
)

private fun ArbeidIPeriode.somMap() : Map<String, Any?> = mapOf(
    "jobberIPerioden" to jobberIPerioden.tilBoolean(),
    "jobberSomVanlig" to jobberSomVanlig,
    "skalViseJobberSomVanlig" to (jobberIPerioden == JobberIPeriodeSvar.JA),
    "erLiktHverUkeSatt" to (erLiktHverUke != null),
    "erLiktHverUke" to erLiktHverUke,
    "enkeltdagerPerMnd" to enkeltdager?.somMapPerMnd(),
    "fasteDager" to fasteDager?.somMap()
)

private fun Frilans.somMap() : Map<String, Any?> = mapOf(
    "startdato" to DATE_FORMATTER.format(startdato),
    "sluttdato" to if(sluttdato != null) DATE_FORMATTER.format(sluttdato) else null,
    "jobberFortsattSomFrilans" to jobberFortsattSomFrilans,
    "arbeidsforhold" to arbeidsforhold?.somMap(
        skalViseHistoriskArbeid = startdato.erFørDagensDato(),
        skalVisePlanlagtArbeid = sluttdato?.erLikEllerEtterDagensDato() ?: true //Hvis vedkommende fortsatt er frilans skal planlagt vises.
    )
)

private fun SelvstendigNæringsdrivende.somMap() : Map<String, Any?> = mapOf(
    "virksomhet" to virksomhet.somMap(),
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

private fun List<ArbeidsforholdAnsatt>.somMapAnsatt() = map {
    mapOf<String, Any?>(
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
        mapOf<String, Any?>(
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
    0 -> "${this.toHoursPart()} timer"
    else -> "${this.toHoursPart()} timer og ${this.toMinutesPart()} minutter"
}

fun Søker.formatertNavn() = if (mellomnavn != null) "$fornavn $mellomnavn $etternavn" else "$fornavn $etternavn"

fun String.capitalizeName(): String = split(" ").joinToString(" ") { name: String ->
    name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private fun String.sprakTilTekst() = when (this.lowercase()) {
    "nb" -> "bokmål"
    "nn" -> "nynorsk"
    else -> this
}

private fun MeldingV1.sjekkOmHarIkkeVedlegg(): Boolean = !vedleggUrls.isNotEmpty()