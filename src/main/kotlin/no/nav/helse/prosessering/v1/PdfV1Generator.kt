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
import no.nav.helse.felles.*
import no.nav.helse.pleiepengerKonfiguert
import no.nav.helse.utils.DateUtils
import no.nav.helse.utils.somNorskDag
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
    private companion object {
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
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZONE_ID)
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZONE_ID)
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
                        "arbeidsgivere" to mapOf(
                            "har_arbeidsgivere" to melding.arbeidsgivere.organisasjoner.isNotEmpty(),
                            "organisasjoner" to melding.arbeidsgivere.organisasjoner.somMap()
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
                            "ingen_arbeidsgivere" to melding.arbeidsgivere.organisasjoner.isEmpty(),
                            "sprak" to melding.språk?.sprakTilTekst()
                        ),
                        "omsorgstilbud" to melding.omsorgstilbud?.somMap(),
                        "omsorgstilbudV2" to melding.omsorgstilbudV2?.somMap(melding.fraOgMed, melding.tilOgMed),
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
                        "skal_bekrefte_omsorg" to melding.skalBekrefteOmsorg,
                        "skal_passe_pa_barnet_i_hele_perioden" to melding.skalPassePaBarnetIHelePerioden,
                        "beskrivelse_omsorgsrollen" to melding.beskrivelseOmsorgsrollen,
                        "barnRelasjon" to melding.barnRelasjon?.utskriftsvennlig,
                        "barnRelasjonBeskrivelse" to melding.barnRelasjonBeskrivelse,
                        "harVærtEllerErVernepliktig" to melding.harVærtEllerErVernepliktig,
                        "frilanserArbeidsforhold" to melding.frilans?.arbeidsforhold?.somMap(),
                        "selvstendigArbeidsforhold" to melding.selvstendigArbeidsforhold?.somMap(),
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

    private fun MeldingV1.harFlereAktiveVirksomehterSatt() =
        (this.selvstendigVirksomheter.firstOrNull()?.harFlereAktiveVirksomheter != null)

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

    private fun Omsorgstilbud.somMap() = mapOf(
        "fasteDager" to fasteDager?.somMap(),
        "vetOmsorgstilbud" to vetOmsorgstilbud.name,
    )

    private fun OmsorgstilbudV2.somMap(fraOgMed: LocalDate, tilOgMed: LocalDate) = mapOf(
        "historisk" to historisk?.somMap(),
        "planlagt" to planlagt?.somMap(),
        "søknadsperiodeFraOgMed" to DATE_FORMATTER.format(fraOgMed),
        "søknadsperiodeTilOgMed" to DATE_FORMATTER.format(tilOgMed),
        "dagensDato" to DATE_FORMATTER.format(LocalDate.now()),
        "igårDato" to DATE_FORMATTER.format(LocalDate.now().minusDays(1)),
        "periodenStarterIFortid" to (fraOgMed.isBefore(LocalDate.now())),
        "periodenAvsluttesIFremtiden" to (tilOgMed.isAfter(LocalDate.now().minusDays(1)))
    )

    private fun List<Omsorgsdag>.somMap(): List<Map<String, Any?>> {
        return map {
            mapOf<String, Any?>(
                "dato" to DATE_FORMATTER.format(it.dato),
                "dag" to it.dato.dayOfWeek.somNorskDag(),
                "tid" to it.tid.somTekst(avkort = false)
            )
        }
    }

    private fun List<Omsorgsdag>.somMapPerUke(): List<Map<String, Any>> {
        val omsorgsdagerPerUke = this.groupBy { it.dato.get(WeekFields.of(Locale.getDefault()).weekOfYear())}
        return omsorgsdagerPerUke.map {
            mapOf(
                "uke" to it.key,
                "dager" to it.value.somMap()
            )
        }
    }

    private fun HistoriskOmsorgstilbud.somMap(): Map<String, Any?> = mutableMapOf(
        "enkeltdagerPerUke" to enkeltdager.somMapPerUke(),
    )

    private fun PlanlagtOmsorgstilbud.somMap(): Map<String, Any?> = mutableMapOf(
        "enkeltdagerPerUke" to enkeltdager?.somMapPerUke(),
        "ukedager" to ukedager?.somMap(),
        "vetOmsorgstilbud" to vetOmsorgstilbud.name,
        "vetLikeDager" to (ukedager != null)
    )

    private fun OmsorgstilbudUkedager.somMap() = mapOf<String, Any?>(
        "mandag" to mandag?.somTekst(),
        "tirsdag" to tirsdag?.somTekst(),
        "onsdag" to onsdag?.somTekst(),
        "torsdag" to torsdag?.somTekst(),
        "fredag" to fredag?.somTekst()
    )

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

private fun Arbeidsforhold.somMap(): Map<String, Any?> = mapOf(
    "skalJobbe" to skalJobbe.verdi,
    "skalJobbeProsent" to skalJobbeProsent.avrundetMedEnDesimal(),
    "inntektstapProsent" to skalJobbeProsent.skalJobbeProsentTilInntektstap(),
    "jobberNormaltTimer" to jobberNormaltTimer,
    "arbeidsform" to arbeidsform.utskriftsvennlig.lowercase()
)

private fun List<Organisasjon>.somMap() = map {
    val skalJobbeProsent = it.skalJobbeProsent.avrundetMedEnDesimal()
    val jobberNormaltimer = it.jobberNormaltTimer
    val inntektstapProsent = skalJobbeProsent.skalJobbeProsentTilInntektstap()
    val vetIkkeEkstrainfo = it.vetIkkeEkstrainfo

    mapOf<String, Any?>(
        "navn" to it.navn,
        "organisasjonsnummer" to it.formaterOrganisasjonsnummer(),
        "skal_jobbe" to it.skalJobbe.verdi,
        "skal_jobbe_prosent" to skalJobbeProsent.formatertMedEnDesimal(),
        "inntektstap_prosent" to inntektstapProsent.formatertMedEnDesimal(),
        "jobber_normaltimer" to jobberNormaltimer,
        "vet_ikke_ekstra_info" to vetIkkeEkstrainfo,
        "arbeidsform" to it.arbeidsform?.utskriftsvennlig?.lowercase()
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
