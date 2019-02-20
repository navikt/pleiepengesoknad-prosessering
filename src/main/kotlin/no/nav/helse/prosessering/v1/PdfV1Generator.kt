package no.nav.helse.prosessering.v1

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.lang.IllegalStateException
import java.time.format.DateTimeFormatter

private val logger: Logger = LoggerFactory.getLogger("nav.PdfV1Generator")
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

/**
 * TODO: Til når vi vet hvordan PDF skal se ut og hva den skal inneholde;
 * - Om vi skal ha "lokal" PDF-genrering bør vi kanskje bruke en template engine som handlebars/freemarker eller lignende?
 * - Ev. kan vi se på å bruke prosjektet "pdfgen" om vi kan deploye vår egen instans av tjenesten
 */
class PdfV1Generator {

    private val SOKNAD_TEMPLATE = "soknad-oppsummering-template.html".fromResources()
    private val ORGANISASJON_ARBEIDSFORHOLD_TEMPLATE = "organisasjon-arbeidsforhold-template.html".fromResources()

    fun generateSoknadOppsummeringPdf(
        melding: MeldingV1
    ) : ByteArray {

        val html = SOKNAD_TEMPLATE
            .med("soker.navn", melding.soker.navn())
            .med("soker.fodselsnummer", melding.soker.fodselsnummer)

            .med("barn.navn", melding.barn.navn)
            .med("barn.fodselsnummer", melding.barn.fodselsnummer)
            .med("barn.alternativ_id", melding.barn.alternativId)

            .med("annet.relasjon_til_barnet", melding.relasjonTilBarnet)
            .med("annet.mottatt", DATE_TIME_FORMATTER.format(melding.mottatt))
            .med("annet.fra_og_med", DATE_FORMATTER.format(melding.mottatt))
            .med("annet.til_og_med", DATE_FORMATTER.format(melding.mottatt))

            .med("medlemskap.har_bodd", melding.medlemskap.harBoddIUtlandetSiste12Mnd.tilJaEllerNei())
            .med("medlemskap.skal_bo", melding.medlemskap.skalBoIUtlandetNeste12Mnd.tilJaEllerNei())

            .medArbeidsgivere(melding.arbeidsgivere)

            .valider()


        val outputStream = ByteArrayOutputStream()

        PdfRendererBuilder()
            .withHtmlContent(html, "")
            .toStream(outputStream)
            .run()

        return outputStream.toByteArray()
    }

    private fun String.fromResources() : String {
        return Thread.currentThread().contextClassLoader.getResource(this).readText()
    }

    private fun String.med(key: String, value: String?) : String {
        return this.replace("{{$key}}", value ?: "n/a")
    }

    private fun String.medArbeidsgivere(arbeidsgivere: Arbeidsgivere) : String {
        var html = ""
        arbeidsgivere.organisasjoner.forEach {
            html = html.plus(ORGANISASJON_ARBEIDSFORHOLD_TEMPLATE
                .med("organisasjonsnummer", it.organisasjonsnummer)
                .med("navn", it.navn)
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

private fun Boolean.tilJaEllerNei(): String {
    return if (this) "JA" else "NEI"
}

private fun Soker.navn(): String? {
    return if (mellomnavn != null) "$fornavn $mellomnavn $etternavn" else "$fornavn $etternavn"
}
