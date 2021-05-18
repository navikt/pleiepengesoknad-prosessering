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
import no.nav.helse.dusseldorf.ktor.core.fromResources
import no.nav.helse.felles.*
import no.nav.helse.pleiepengerKonfiguert
import no.nav.helse.utils.DateUtils
import no.nav.helse.utils.fødselsdato
import no.nav.helse.utils.norskDag
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

internal class PdfV1Generator {
    private companion object {
        private val mapper = jacksonObjectMapper().pleiepengerKonfiguert()

        private const val ROOT = "handlebars"
        private const val SOKNAD = "soknad"

        private val REGULAR_FONT = "$ROOT/fonts/SourceSansPro-Regular.ttf".fromResources().readBytes()
        private val BOLD_FONT = "$ROOT/fonts/SourceSansPro-Bold.ttf".fromResources().readBytes()
        private val ITALIC_FONT = "$ROOT/fonts/SourceSansPro-Italic.ttf".fromResources().readBytes()


        private val images = loadImages()
        private val handlebars = Handlebars(ClassPathTemplateLoader("/$ROOT")).apply {
            registerHelper("image", Helper<String> { context, _ ->
                if (context == null) "" else images[context]
            })
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
                context.capitalize()
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

        private fun loadPng(name: String): String {
            val bytes = "$ROOT/images/$name.png".fromResources().readBytes()
            val base64string = Base64.getEncoder().encodeToString(bytes)
            return "data:image/png;base64,$base64string"
        }

        private fun loadImages() = mapOf(
            "Checkbox_off.png" to loadPng("Checkbox_off"),
            "Checkbox_on.png" to loadPng("Checkbox_on"),
            "Hjelp.png" to loadPng("Hjelp"),
            "Navlogo.png" to loadPng("Navlogo"),
            "Personikon.png" to loadPng("Personikon"),
            "Fritekst.png" to loadPng("Fritekst")
        )
    }

    internal fun generateSoknadOppsummeringPdf(
        melding: MeldingV1
    ): ByteArray {
        soknadTemplate.apply(
            Context
                .newBuilder(
                    mapOf(
                        "søknad" to melding.somMap(),
                        "soknad_id" to melding.søknadId,
                        "soknad_mottatt_dag" to melding.mottatt.withZoneSameInstant(ZONE_ID).norskDag(),
                        "soknad_mottatt" to DATE_TIME_FORMATTER.format(melding.mottatt),
                        "har_medsoker" to melding.harMedsøker,
                        "harIkkeVedlegg" to melding.sjekkOmHarIkkeVedlegg(),
                        "samtidig_hjemme" to melding.samtidigHjemme,
                        "bekrefterPeriodeOver8Uker" to melding.bekrefterPeriodeOver8Uker,
                        "soker" to mapOf(
                            "navn" to melding.søker.formatertNavn().capitalizeName(),
                            "fodselsnummer" to melding.søker.fødselsnummer
                        ),
                        "barn" to mapOf(
                            "navn" to melding.barn.navn.capitalizeName(),
                            "fodselsdato" to melding.barn.fødselsdato().format(DATE_FORMATTER),
                            "id" to melding.barn.fødselsnummer
                        ),
                        "periode" to mapOf(
                            "fra_og_med" to DATE_FORMATTER.format(melding.fraOgMed),
                            "til_og_med" to DATE_FORMATTER.format(melding.tilOgMed),
                            "virkedager" to DateUtils.antallVirkedager(melding.fraOgMed, melding.tilOgMed)
                        ),
                        "arbeidsgivere" to mapOf(
                            "har_arbeidsgivere" to melding.arbeidsgivere.organisasjoner.isNotEmpty(),
                            "aktuelle_arbeidsgivere" to melding.arbeidsgivere.organisasjoner.erAktuelleArbeidsgivere(),
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
                        "tilsynsordning" to tilsynsordning(melding.tilsynsordning),
                        "omsorgstilbud" to melding.omsorgstilbud?.somMap(),
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
                    )
                )
                .resolver(MapValueResolver.INSTANCE)
                .build()
        ).let { html ->
            val outputStream = ByteArrayOutputStream()

            PdfRendererBuilder()
                .useFastMode()
                .usePdfUaAccessbility(true)
                .withHtmlContent(html, "")
                .medFonter()
                .toStream(outputStream)
                .buildPdfRenderer()
                .createPDF()

            return outputStream.use {
                it.toByteArray()
            }
        }
    }

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

    private fun OmsorgstilbudFasteDager.somMap() = mapOf<String, Any?>(
        "mandag" to mandag?.somTekst(),
        "tirsdag" to tirsdag?.somTekst(),
        "onsdag" to onsdag?.somTekst(),
        "torsdag" to torsdag?.somTekst(),
        "fredag" to fredag?.somTekst()
    )

    private fun tilsynsordning(tilsynsordning: Tilsynsordning?) = when {
        tilsynsordning == null -> null
        "ja" == tilsynsordning.svar -> mapOf(
            "tilsynsordning_svar" to "ja",
            "mandag" to tilsynsordning.ja?.mandag?.somTekst(),
            "tirsdag" to tilsynsordning.ja?.tirsdag?.somTekst(),
            "onsdag" to tilsynsordning.ja?.onsdag?.somTekst(),
            "torsdag" to tilsynsordning.ja?.torsdag?.somTekst(),
            "fredag" to tilsynsordning.ja?.fredag?.somTekst(),
            "tilleggsinformasjon" to tilsynsordning.ja?.tilleggsinformasjon,
            "prosent_av_normal_arbeidsuke" to tilsynsordning.ja?.prosentAvNormalArbeidsuke()?.formatertMedEnDesimal()
        )
        "vetIkke" == tilsynsordning.svar -> mapOf(
            "tilsynsordning_svar" to "vetIkke",
            "svar" to tilsynsordning.vetIkke?.svar,
            "annet" to tilsynsordning.vetIkke?.annet
        )
        "nei" == tilsynsordning.svar -> mapOf(
            "tilsynsordning_svar" to "nei"
        )
        else -> null
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

private fun Arbeidsforhold.somMap(): Map<String, Any?> = mapOf(
    "skalJobbe" to skalJobbe.verdi,
    "skalJobbeProsent" to skalJobbeProsent.avrundetMedEnDesimal(),
    "inntektstapProsent" to skalJobbeProsent.skalJobbeProsentTilInntektstap(),
    "jobberNormaltTimer" to jobberNormaltTimer,
    "arbeidsform" to arbeidsform.utskriftsvennlig.toLowerCase()
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
        "arbeidsform" to it.arbeidsform?.utskriftsvennlig
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

private fun List<Organisasjon>.erAktuelleArbeidsgivere() = any { it.skalJobbeProsent != null }

private fun Duration.tilString(): String = when (this.toMinutesPart()) {
    0 -> "${this.toHoursPart()} timer"
    else -> "${this.toHoursPart()} timer og ${this.toMinutesPart()} minutter"
}

private fun Søker.formatertNavn() = if (mellomnavn != null) "$fornavn $mellomnavn $etternavn" else "$fornavn $etternavn"

fun String.capitalizeName(): String = split(" ").joinToString(" ") { it.toLowerCase().capitalize() }

private fun String.sprakTilTekst() = when (this.toLowerCase()) {
    "nb" -> "bokmål"
    "nn" -> "nynorsk"
    else -> this
}

private fun MeldingV1.sjekkOmHarIkkeVedlegg(): Boolean = !vedleggUrls.isNotEmpty()
