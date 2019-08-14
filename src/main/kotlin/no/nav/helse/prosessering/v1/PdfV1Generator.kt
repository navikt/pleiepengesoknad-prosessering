package no.nav.helse.prosessering.v1

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.context.MapValueResolver
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import io.ktor.util.extension
import no.nav.helse.dusseldorf.ktor.core.fromResources
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.streams.toList

internal class PdfV1Generator  {
    private companion object {
        private const val ROOT = "handlebars"
        private const val SOKNAD = "soknad"

        private val REGULAR_FONT = "$ROOT/fonts/SourceSansPro-Regular.ttf".fromResources().readBytes()
        private val BOLD_FONT = "$ROOT/fonts/SourceSansPro-Bold.ttf".fromResources().readBytes()
        private val ITALIC_FONT = "$ROOT/fonts/SourceSansPro-Italic.ttf".fromResources().readBytes()

        private val imagesRoot = Paths.get("$ROOT/images".fromResources().toURI())

        private val images = loadImages()
        private val handlebars = Handlebars(ClassPathTemplateLoader("/$ROOT")).apply {
            registerHelper("image", Helper<String> { context, _ ->
                if (context == null) "" else images[context]
            })
            infiniteLoops(true)
        }

        private val soknadTemplate = handlebars.compile(SOKNAD)

        private val ZONE_ID = ZoneId.of("Europe/Oslo")
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZONE_ID)
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZONE_ID)

        private fun loadImages() = Files.list(imagesRoot)
            .filter {
                val validExtensions = setOf("jpg", "jpeg", "png")
                !Files.isHidden(it) && it.fileName.extension in validExtensions
            }
            .map {
                val fileName = it.fileName.toString()
                val extension = when (it.fileName.extension) {
                    "jpg" -> "jpeg" // jpg is not a valid mime-type
                    else -> it.fileName.extension
                }
                val base64string = Base64.getEncoder().encodeToString(Files.readAllBytes(it))
                val base64 = "data:image/$extension;base64,$base64string"
                fileName to base64
            }
            .toList()
            .toMap()
    }

    internal fun generateSoknadOppsummeringPdf(
        melding: MeldingV1
    ) : ByteArray {

        soknadTemplate.apply(Context
            .newBuilder(mapOf(
                "sprak" to melding.sprak?.sprakTilTekst(),
                "soknad_id" to melding.soknadId,
                "soknad_mottatt_dag" to melding.mottatt.withZoneSameInstant(ZONE_ID).norskDag(),
                "soknad_mottatt" to DATE_TIME_FORMATTER.format(melding.mottatt),
                "har_medsoker" to melding.harMedsoker,
                "grad" to melding.grad,
                "soker" to mapOf(
                    "navn" to melding.soker.formatertNavn(),
                    "fodselsnummer" to melding.soker.formatertFodselsnummer(),
                    "relasjon_til_barnet" to melding.relasjonTilBarnet
                ),
                "barn" to mapOf(
                    "navn" to melding.barn.navn,
                    "id" to melding.barn.formatertId()
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
                    "skal_bo_i_utlandet_neste_12_mnd" to melding.medlemskap.skalBoIUtlandetNeste12Mnd
                ),
                "samtykke" to mapOf(
                    "har_forstatt_rettigheter_og_plikter" to melding.harForstattRettigheterOgPlikter,
                    "har_bekreftet_opplysninger" to melding.harBekreftetOpplysninger
                )
            ))
            .resolver(MapValueResolver.INSTANCE)
            .build()).let { html ->
            val outputStream = ByteArrayOutputStream()

            PdfRendererBuilder()
                .useFastMode()
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

    private fun PdfRendererBuilder.medFonter() =
        useFont({ ByteArrayInputStream(REGULAR_FONT) }, "Source Sans Pro", 400, BaseRendererBuilder.FontStyle.NORMAL, false)
        .useFont({ ByteArrayInputStream(BOLD_FONT) }, "Source Sans Pro", 700, BaseRendererBuilder.FontStyle.NORMAL, false)
        .useFont({ ByteArrayInputStream(ITALIC_FONT) }, "Source Sans Pro", 400, BaseRendererBuilder.FontStyle.ITALIC, false)
}

private fun List<Organisasjon>.somMap() = map {mapOf<String,Any?>(
        "navn" to it.navn,
        "organisasjonsnummer" to it.organisasjonsnummer,
        "gradering" to GraderingUtils.omArbeidsgiversGradering(ArbeidsgiverUtils.prosentAvNormalArbeidsuke(it.normalArbeidsuke, it.redusertArbeidsuke))
    )
}

private fun String.formaterId() = "${this.substring(0,6)} ${this.substring(6)}"
private fun Soker.formatertFodselsnummer() = this.fodselsnummer.formaterId()
private fun Barn.formatertId() : String? {
    return if (fodselsnummer != null || alternativId != null) (fodselsnummer?:alternativId)!!.formaterId()
    else null
}
private fun Soker.formatertNavn() = if (mellomnavn != null) "$fornavn $mellomnavn $etternavn" else "$fornavn $etternavn"
private fun String.sprakTilTekst() = when (this.toLowerCase()) {
    "nb" -> "Bokmål"
    "nn" -> "Nynorsk"
    else -> this
}