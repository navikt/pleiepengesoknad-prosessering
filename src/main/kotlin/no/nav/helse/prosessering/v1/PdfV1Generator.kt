package no.nav.helse.prosessering.v1

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.slf4j.Slf4jLogger

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.lang.IllegalStateException
import java.time.format.DateTimeFormatter
import com.openhtmltopdf.util.XRLog
import java.time.ZoneId

private val logger: Logger = LoggerFactory.getLogger("nav.PdfV1Generator")
private val ZONE_ID = ZoneId.of("Europe/Oslo")
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZONE_ID)
private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZONE_ID)

private val HTML_RESERVED_CHARACTERS_REGEX = """['"<>&]""".toRegex()
private val HTML_RESERVED_CHARACTERS_MAPPING = linkedMapOf(
    "&" to "&amp;",
    "\"" to "&quot;",
    "'" to "&apos;",
    "<" to "&lt;",
    ">" to "&gt;"
)

/**
 * TODO: Til når vi vet hvordan PDF skal se ut og hva den skal inneholde;
 * - Om vi skal ha "lokal" PDF-genrering bør vi kanskje bruke en template engine som handlebars/freemarker eller lignende?
 * - Ev. kan vi se på å bruke prosjektet "pdfgen" om vi kan deploye vår egen instans av tjenesten
 */
class PdfV1Generator {

    private companion object {
        private val SOKNAD_TEMPLATE = "soknad-oppsummering-template.html".fromResources()
        private val ORGANISASJON_ARBEIDSFORHOLD_TEMPLATE = "organisasjon-arbeidsforhold-template.html".fromResources()
        private val BASE_URL = Thread.currentThread().contextClassLoader.getResource("img")!!.toString()

        private fun String.fromResources() : String {
            return Thread.currentThread().contextClassLoader.getResource(this)!!.readText(Charsets.UTF_8)
        }
    }

    init {
        XRLog.setLoggerImpl(Slf4jLogger())
    }

    fun generateSoknadOppsummeringPdf(
        melding: MeldingV1
    ) : ByteArray {

        val html = SOKNAD_TEMPLATE
            .med("soknadId", melding.soknadId)
            .med("soker.navn", melding.soker.navn())
            .med("soker.fodselsnummer", melding.soker.fodselsnummer)

            .med("barn.navn", melding.barn.navn)
            .med("barn.fodselsnummer", melding.barn.fodselsnummer)
            .med("barn.alternativ_id", melding.barn.alternativId)

            .med("annet.sprak", melding.sprak?.somTekst())
            .med("annet.relasjon_til_barnet", melding.relasjonTilBarnet)
            .med("annet.mottatt", DATE_TIME_FORMATTER.format(melding.mottatt))
            .med("annet.fra_og_med", DATE_FORMATTER.format(melding.fraOgMed))
            .med("annet.til_og_med", DATE_FORMATTER.format(melding.tilOgMed))
            .med("annet.grad", melding.grad.toString())
            .med("annet.har_medsoker", melding.harMedsoker.tilJaEllerNei())
            .med("annet.har_forstatt_rettigheter_og_plikter", melding.harForstattRettigheterOgPlikter.tilJaEllerNei())
            .med("annet.har_bekreftet_opplysninger", melding.harBekreftetOpplysninger.tilJaEllerNei())

            .med("medlemskap.har_bodd", melding.medlemskap.harBoddIUtlandetSiste12Mnd.tilJaEllerNei())
            .med("medlemskap.skal_bo", melding.medlemskap.skalBoIUtlandetNeste12Mnd.tilJaEllerNei())

            .medArbeidsgivere(melding.arbeidsgivere)

            .valider()

        val outputStream = ByteArrayOutputStream()

        PdfRendererBuilder()
            .withHtmlContent(html, BASE_URL)
            .toStream(outputStream)
            .buildPdfRenderer()
            .createPDF()

        return outputStream.use {
            it.toByteArray()
        }
    }

    private fun String.med(key: String, value: String?) : String {
        val replaceValue =
            if (value == null) "n/a"
            else if (!value.contains(HTML_RESERVED_CHARACTERS_REGEX)) value
            else {
            var newValue : String = value
            HTML_RESERVED_CHARACTERS_MAPPING.forEach{ char, html ->
                newValue = newValue.replace(char, html)
            }
            newValue
        }
        return this.replace("{{$key}}", replaceValue)
    }

    private fun String.medArbeidsgivere(arbeidsgivere: Arbeidsgivere) : String {
        var html = ""
        arbeidsgivere.organisasjoner.forEach {
            html = html.plus(ORGANISASJON_ARBEIDSFORHOLD_TEMPLATE
                .med("organisasjonsnummer", it.organisasjonsnummer)
                .med("navn", it.navn)
                .med("arbeidsuker", ArbeidsgiverUtils.formaterArbeidsuker(it.normalArbeidsuke, it.redusertArbeidsuke)?: "Ingen detaljer angitt for denne arbeidsgiveren.")
            )
        }
        return this.replace("{{arbeidsgivere}}", html)
    }

    private fun String.valider() : String {
        if (this.contains("{{") || this.contains("}}")) {
            logger.info("Ugyldig HTML = $this")
            throw IllegalStateException("Ugyldig HTML")
        }
        return this
    }
}

private fun String.somTekst() = when (this.toLowerCase()) {
    "nb" -> "Bokmål"
    "nn" -> "Nynorsk"
    else -> this
}

private fun Boolean.tilJaEllerNei(): String {
    return if (this) "Ja" else "Nei"
}

private fun Soker.navn(): String? {
    return if (mellomnavn != null) "$fornavn $mellomnavn $etternavn" else "$fornavn $etternavn"
}