package no.nav.helse.pdf

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Template
import com.github.jknack.handlebars.context.MapValueResolver
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import no.nav.helse.dusseldorf.ktor.core.fromResources
import no.nav.helse.felles.Næringstyper
import no.nav.helse.pleiepengerKonfiguert
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Baseklasse for generering av pdf.
 *
 * Bruk: Implementer denne klassen med type T som ønskes generert på pdf.
 *
 * @property templateNavn er navnet på handlebars template-filen.
 * Altså har du en handlebars template fil ved navn søknad.hbs, skal `templateNavn` være søknad.
 *
 * @property bilder laster inn bilder man ønsker skal kunne brukes i templaten.
 *
 * @property tilMap metoden mapper opp instansen T til en map som kan parses i templaten.
 */
abstract class PDFGenerator<in T> {
    companion object {
        val ZONE_ID: ZoneId = ZoneId.of("Europe/Oslo")
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZONE_ID)
        val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZONE_ID)
    }

    protected abstract val templateNavn: String
    protected abstract val bilder: Map<String, String>

    protected val mapper = jacksonObjectMapper().pleiepengerKonfiguert()

    private val ROOT = "handlebars"
    private val REGULAR_FONT = "${ROOT}/fonts/SourceSansPro-Regular.ttf".fromResources().readBytes()
    private val BOLD_FONT = "${ROOT}/fonts/SourceSansPro-Bold.ttf".fromResources().readBytes()
    private val ITALIC_FONT = "${ROOT}/fonts/SourceSansPro-Italic.ttf".fromResources().readBytes()
    protected val handlebars = configureHandlebars()
    private val søknadsTemplate: Template = handlebars.compile(templateNavn)

    abstract fun T.tilMap(): Map<String, Any?>

    fun genererPDF(melding: T): ByteArray = søknadsTemplate.apply(
        Context
            .newBuilder(melding.tilMap())
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

        outputStream.use {
            it.toByteArray()
        }
    }

    protected fun loadPng(name: String): String {
        val bytes = "${ROOT}/images/$name.png".fromResources().readBytes()
        val base64string = Base64.getEncoder().encodeToString(bytes)
        return "data:image/png;base64,$base64string"
    }

    private fun configureHandlebars(): Handlebars {
        return Handlebars(ClassPathTemplateLoader("/${ROOT}")).apply {
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
}
