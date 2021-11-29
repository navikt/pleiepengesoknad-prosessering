package no.nav.helse.prosessering.v1

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.context.MapValueResolver
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.util.XRLog
import no.nav.helse.dusseldorf.ktor.core.fromResources
import no.nav.helse.felles.ArbeidIPeriode
import no.nav.helse.felles.Arbeidsforhold
import no.nav.helse.felles.Beredskap
import no.nav.helse.felles.Bosted
import no.nav.helse.felles.Enkeltdag
import no.nav.helse.felles.Ferieuttak
import no.nav.helse.felles.Frilans
import no.nav.helse.felles.HistoriskOmsorgstilbud
import no.nav.helse.felles.JobberIPeriodeSvar
import no.nav.helse.felles.Land
import no.nav.helse.felles.Nattevåk
import no.nav.helse.felles.Næringstyper
import no.nav.helse.felles.Periode
import no.nav.helse.felles.PlanUkedager
import no.nav.helse.felles.PlanlagtOmsorgstilbud
import no.nav.helse.felles.Regnskapsfører
import no.nav.helse.felles.SelvstendigNæringsdrivende
import no.nav.helse.felles.Søker
import no.nav.helse.felles.Utenlandsopphold
import no.nav.helse.felles.VarigEndring
import no.nav.helse.felles.Virksomhet
import no.nav.helse.felles.YrkesaktivSisteTreFerdigliknedeÅrene
import no.nav.helse.pleiepengerKonfiguert
import no.nav.helse.prosessering.v1.PdfV1Generator.Companion.DATE_FORMATTER
import no.nav.helse.utils.DateUtils
import no.nav.helse.utils.somNorskDag
import no.nav.helse.utils.somNorskMåned
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*
import java.util.logging.Level

internal class PdfV1Generator {
    companion object {
        private val mapper = jacksonObjectMapper().pleiepengerKonfiguert()

        private const val ROOT = "handlebars"
        private const val SOKNAD = "soknad"

        private val REGULAR_FONT = "$ROOT/fonts/SourceSansPro-Regular.ttf".fromResources().readBytes()
        private val BOLD_FONT = "$ROOT/fonts/SourceSansPro-Bold.ttf".fromResources().readBytes()
        private val ITALIC_FONT = "$ROOT/fonts/SourceSansPro-Italic.ttf".fromResources().readBytes()

        private val sRGBColorSpace = "$ROOT/sRGB.icc".fromResources().readBytes()


        private val handlebars = Handlebars(ClassPathTemplateLoader("/$ROOT")).apply {
            registerHelper("eq", Helper<String> { context, options ->
                if (context == options.param(0)) options.fn() else options.inverse()
            })
            registerHelper("fritekst", Helper<String> { context, _ ->
                if (context == null) "" else {
                    val text = Handlebars.Utils.escapeExpression(context)
                        .toString()
                        .replace(Regex("\\r\\n|[\\n\\r]"), "<br/>")
                    Handlebars.SafeString(text)
                }
            })
            registerHelper("enumNæringstyper", Helper<String> { context, _ ->
                Næringstyper.valueOf(context).beskrivelse
            })
            registerHelper("dato", Helper<String> { context, _ ->
                DATE_FORMATTER.format(LocalDate.parse(context))
            })
            registerHelper("storForbokstav", Helper<String> { context, _ ->
                context.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            })
            registerHelper("tidspunkt", Helper<String> { context, _ ->
                DATE_TIME_FORMATTER.format(ZonedDateTime.parse(context))
            })
            registerHelper("varighet", Helper<String> { context, _ ->
                Duration.parse(context).tilString()
            })
            registerHelper("jaNeiSvar", Helper<Boolean> { context, _ ->
                if (context == true) "Ja" else "Nei"
            })

            infiniteLoops(true)
        }

        private val soknadTemplate = handlebars.compile(SOKNAD)

        private val ZONE_ID = ZoneId.of("Europe/Oslo")
        val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZONE_ID)
        val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZONE_ID)
    }

    internal fun generateSoknadOppsummeringPdf(
        melding: MeldingV1
    ): ByteArray {
        XRLog.listRegisteredLoggers().forEach { logger -> XRLog.setLevel(logger, Level.WARNING) }
        soknadTemplate.apply(
            Context
                .newBuilder(
                    mapOf(
                        "søknad" to melding.somMap(),
                        "soknad_id" to melding.søknadId,
                        "soknad_mottatt_dag" to melding.mottatt.withZoneSameInstant(ZONE_ID).somNorskDag(),
                        "soknad_mottatt" to DATE_TIME_FORMATTER.format(melding.mottatt),
                        "har_medsoker" to melding.harMedsøker,
                        "harIkkeVedlegg" to melding.sjekkOmHarIkkeVedlegg(),
                        "samtidig_hjemme" to melding.samtidigHjemme,
                        "soker" to mapOf(
                            "navn" to melding.søker.formatertNavn().capitalizeName(),
                            "fodselsnummer" to melding.søker.fødselsnummer
                        ),
                        "barn" to mapOf(
                            "id" to melding.barn.fødselsnummer,
                            "navn" to melding.barn.navn.capitalizeName()
                        ),
                        "periode" to mapOf(
                            "fra_og_med" to DATE_FORMATTER.format(melding.fraOgMed),
                            "til_og_med" to DATE_FORMATTER.format(melding.tilOgMed),
                            "virkedager" to DateUtils.antallVirkedager(melding.fraOgMed, melding.tilOgMed)
                        ),
                        "medlemskap" to mapOf(
                            "har_bodd_i_utlandet_siste_12_mnd" to melding.medlemskap.harBoddIUtlandetSiste12Mnd,
                            "utenlandsopphold_siste_12_mnd" to melding.medlemskap.utenlandsoppholdSiste12Mnd.somMapBosted(),
                            "skal_bo_i_utlandet_neste_12_mnd" to melding.medlemskap.skalBoIUtlandetNeste12Mnd,
                            "utenlandsopphold_neste_12_mnd" to melding.medlemskap.utenlandsoppholdNeste12Mnd.somMapBosted()
                        ),
                        "samtykke" to mapOf(
                            "har_forstatt_rettigheter_og_plikter" to melding.harForståttRettigheterOgPlikter,
                            "har_bekreftet_opplysninger" to melding.harBekreftetOpplysninger
                        ),
                        "hjelp" to mapOf(
                            "har_medsoker" to melding.harMedsøker,
                            "ingen_arbeidsgivere" to (melding.arbeidsgivere == null),
                            "sprak" to melding.språk?.sprakTilTekst()
                        ),
                        "omsorgstilbud" to melding.omsorgstilbudSomMap(melding.fraOgMed, melding.tilOgMed),
                        "nattevaak" to nattevåk(melding.nattevåk),
                        "beredskap" to beredskap(melding.beredskap),
                        "utenlandsoppholdIPerioden" to mapOf(
                            "skalOppholdeSegIUtlandetIPerioden" to melding.utenlandsoppholdIPerioden.skalOppholdeSegIUtlandetIPerioden,
                            "opphold" to melding.utenlandsoppholdIPerioden.opphold.somMapUtenlandsopphold()
                        ),
                        "ferieuttakIPerioden" to mapOf(
                            "skalTaUtFerieIPerioden" to melding.ferieuttakIPerioden?.skalTaUtFerieIPerioden,
                            "ferieuttak" to melding.ferieuttakIPerioden?.ferieuttak?.somMapFerieuttak()
                        ),
                        "barnRelasjon" to melding.barnRelasjon?.utskriftsvennlig,
                        "barnRelasjonBeskrivelse" to melding.barnRelasjonBeskrivelse,
                        "harVærtEllerErVernepliktig" to melding.harVærtEllerErVernepliktig,
                        "frilans" to melding.frilans?.somMap(),
                        "selvstendigNæringsdrivende" to melding.selvstendigNæringsdrivende?.somMap(),
                        "arbeidsgivere" to melding.arbeidsgivere?.somMapAnsatt(),
                        "hjelper" to mapOf( // TODO: 04/06/2021 Kan fjerne hjelpemetoden når feltet er prodsatt i api og front
                            "harFlereAktiveVirksomheterErSatt" to melding.harFlereAktiveVirksomehterSatt(),
                            "harVærtEllerErVernepliktigErSatt" to erBooleanSatt(melding.harVærtEllerErVernepliktig)
                        )
                    )
                )
                .resolver(MapValueResolver.INSTANCE)
                .build()
        ).let { html ->
            val outputStream = ByteArrayOutputStream()

            PdfRendererBuilder()
                .useFastMode()
                .usePdfUaAccessbility(true)
                .usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_1_B)
                .withHtmlContent(html, "")
                .medFonter()
                .useColorProfile(sRGBColorSpace)
                .toStream(outputStream)
                .buildPdfRenderer()
                .createPDF()

            return outputStream.use {
                it.toByteArray()
            }
        }
    }

    private fun PdfRendererBuilder.medFonter() =
        useFont(
            { ByteArrayInputStream(REGULAR_FONT) },
            "Source Sans Pro",
            400,
            BaseRendererBuilder.FontStyle.NORMAL,
            false
        )
            .useFont(
                { ByteArrayInputStream(BOLD_FONT) },
                "Source Sans Pro",
                700,
                BaseRendererBuilder.FontStyle.NORMAL,
                false
            )
            .useFont(
                { ByteArrayInputStream(ITALIC_FONT) },
                "Source Sans Pro",
                400,
                BaseRendererBuilder.FontStyle.ITALIC,
                false
            )

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
    "normaltTimerPerUkedag" to jobberNormaltTimer.div(5),
    "historiskArbeid" to historiskArbeid?.somMap(),
    "planlagtArbeid" to planlagtArbeid?.somMap(),
    "skalViseHistoriskArbeid" to skalViseHistoriskArbeid,
    "skalVisePlanlagtArbeid" to skalVisePlanlagtArbeid
)

private fun ArbeidIPeriode.somMap() : Map<String, Any?> = mapOf(
    "jobberIPerioden" to jobberIPerioden.tilBoolean(),
    "jobberSomVanlig" to jobberSomVanlig,
    "jobberProsent" to jobberProsent,
    "skalViseJobberSomVanlig" to (jobberIPerioden == JobberIPeriodeSvar.JA),
    "erLiktHverUkeSatt" to (erLiktHverUke != null),
    "erLiktHverUke" to erLiktHverUke,
    "enkeltdagerPerMnd" to enkeltdager?.somMapPerMnd(),
    "fasteDager" to fasteDager?.somMap(),
    "snittTimerPerUkedag" to fasteDager?.mandag?.somTekst() // alle dager er like dersom jobberProsent er satt.
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
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.of("Europe/Oslo"))
    return map {
        mapOf<String, Any?>(
            "landnavn" to it.landnavn,
            "fraOgMed" to dateFormatter.format(it.fraOgMed),
            "tilOgMed" to dateFormatter.format(it.tilOgMed)
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
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.of("Europe/Oslo"))
    return map {
        mapOf<String, Any?>(
            "fraOgMed" to dateFormatter.format(it.fraOgMed),
            "tilOgMed" to dateFormatter.format(it.tilOgMed)
        )
    }
}

private fun List<Periode>.somMapPerioder(): List<Map<String, Any?>> {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.of("Europe/Oslo"))
    return map {
        mapOf<String, Any?>(
            "fraOgMed" to dateFormatter.format(it.fraOgMed),
            "tilOgMed" to dateFormatter.format(it.tilOgMed)
        )
    }
}

private fun Duration.tilString(): String = when (this.toMinutesPart()) {
    0 -> "${this.toHoursPart()} timer"
    else -> "${this.toHoursPart()} timer og ${this.toMinutesPart()} minutter"
}

private fun Søker.formatertNavn() = if (mellomnavn != null) "$fornavn $mellomnavn $etternavn" else "$fornavn $etternavn"

fun String.capitalizeName(): String = split(" ").joinToString(" ") { name: String ->
    name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private fun String.sprakTilTekst() = when (this.lowercase()) {
    "nb" -> "bokmål"
    "nn" -> "nynorsk"
    else -> this
}

private fun MeldingV1.sjekkOmHarIkkeVedlegg(): Boolean = !vedleggUrls.isNotEmpty()
