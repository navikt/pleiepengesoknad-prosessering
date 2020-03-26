package no.nav.helse.prosessering.v1

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.context.MapValueResolver
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import no.nav.helse.aktoer.NorskIdent
import no.nav.helse.dusseldorf.ktor.core.fromResources
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

internal class PdfV1Generator  {
    private companion object {
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
        melding: MeldingV1,
        barnetsIdent: NorskIdent?,
        fodselsdato: LocalDate?,
        barnetsNavn: String?
    ) : ByteArray {
        soknadTemplate.apply(Context
            .newBuilder(mapOf(
                "soknad_id" to melding.soknadId,
                "soknad_mottatt_dag" to melding.mottatt.withZoneSameInstant(ZONE_ID).norskDag(),
                "soknad_mottatt" to DATE_TIME_FORMATTER.format(melding.mottatt),
                "har_medsoker" to melding.harMedsoker,
                "samtidig_hjemme" to melding.samtidigHjemme,
                "bekrefterPeriodeOver8Uker" to melding.bekrefterPeriodeOver8Uker,
                "soker" to mapOf(
                    "navn" to melding.soker.formatertNavn(),
                    "fodselsnummer" to melding.soker.fodselsnummer,
                    "relasjon_til_barnet" to melding.relasjonTilBarnet
                ),
                "barn" to mapOf(
                    "navn" to barnetsNavn,
                    "fodselsdato" to fodselsdato?.format(DATE_FORMATTER),
                    "id" to barnetsIdent?.getValue()
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
                    "har_forstatt_rettigheter_og_plikter" to melding.harForstattRettigheterOgPlikter,
                    "har_bekreftet_opplysninger" to melding.harBekreftetOpplysninger
                ),
                "hjelp" to mapOf(
                    "har_medsoker" to melding.harMedsoker,
                    "ingen_arbeidsgivere" to melding.arbeidsgivere.organisasjoner.isEmpty(),
                    "sprak" to melding.sprak?.sprakTilTekst()
                ),
                "tilsynsordning" to tilsynsordning(melding.tilsynsordning),
                "nattevaak" to nattevåk(melding.nattevaak),
                "beredskap" to beredskap(melding.beredskap),
                "utenlandsoppholdIPerioden" to mapOf(
                    "skalOppholdeSegIUtlandetIPerioden" to melding.utenlandsoppholdIPerioden?.skalOppholdeSegIUtlandetIPerioden,
                    "opphold" to melding.utenlandsoppholdIPerioden?.opphold?.somMapUtenlandsopphold()
                ),
                "ferieuttakIPerioden" to mapOf(
                    "skalTaUtFerieIPerioden" to melding.ferieuttakIPerioden?.skalTaUtFerieIPerioden,
                    "ferieuttak" to melding.ferieuttakIPerioden?.ferieuttak?.somMapFerieuttak()
                ),
                "frilans" to frilans(melding.frilans),
                "selvstendigVirksomheter" to mapOf(
                    "virksomhet" to melding.selvstendigVirksomheter?.somMapVirksomheter()
                ),
                "skal_bekrefte_omsorg" to melding.skalBekrefteOmsorg,
                "skal_passe_pa_barnet_i_hele_perioden" to melding.skalPassePaBarnetIHelePerioden,
                "beskrivelse_omsorgsrollen" to melding.beskrivelseOmsorgsRollen
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

    private fun List<Virksomhet>.somMapVirksomheter(): List<Map<String, Any?>> {
        return map {
            mapOf(
                "navnPaVirksomheten" to it.navnPaVirksomheten,
                "naringstype" to it.naringstype.somMapNaringstype(),
                "fiskerErPåBladB" to it.fiskerErPåBladB,
                "fraOgMed" to it.fraOgMed,
                "tilOgMed" to it.tilOgMed,
                "naringsinntekt" to it.naringsinntekt,
                "registrertINorge" to it.registrertINorge,
                "organisasjonsnummer" to it.organisasjonsnummer,
                "registrertILand" to it.registrertILand,
                "yrkesaktivSisteTreFerdigliknedeArene" to it.yrkesaktivSisteTreFerdigliknedeArene?.oppstartsdato,
                "harVarigEndringAvInntektSiste4Kalenderar" to it.harVarigEndringAvInntektSiste4Kalenderar,
                "varigEndring" to varigEndring(it.varigEndring),
                "harRegnskapsforer" to it.harRegnskapsforer,
                "harRevisor" to it.harRevisor,
                "regnskapsforer" to regnskapsforer(it.regnskapsforer),
                "revisor" to revisor(it.revisor)
            )
        }
    }

    private fun List<Naringstype>.somMapNaringstype(): List<Map<String, Any?>> {
        return map {
            mapOf(
                "typeDetaljert" to it.detaljert
            )
        }
    }

    private fun varigEndring(varigEndring: VarigEndring?) = when (varigEndring) {
        null -> null
        else -> {
            mapOf(
                "dato" to varigEndring.dato,
                "inntektEtterEndring" to varigEndring.inntektEtterEndring,
                "forklaring" to varigEndring.forklaring
            )
        }
    }

    private fun revisor(revisor: Revisor?) = when (revisor) {
        null -> null
        else -> {
            mapOf(
                "navn" to revisor.navn,
                "telefon" to revisor.telefon,
                "kanInnhenteOpplysninger" to revisor.kanInnhenteOpplysninger
            )
        }
    }

    private fun regnskapsforer(regnskapsforer: Regnskapsforer?) = when (regnskapsforer) {
        null -> null
        else -> {
            mapOf(
                "navn" to regnskapsforer.navn,
                "telefon" to regnskapsforer.telefon
            )
        }
    }

    private fun frilans(frilans: Frilans?) = when {
        frilans == null -> null
        else -> {
            mapOf(
                "startdato" to frilans.startdato,
                "jobberFortsattSomFrilans" to frilans.jobberFortsattSomFrilans
            )
        }
    }

    private fun nattevåk(nattevaak: Nattevaak?) = when {
        nattevaak == null -> null
        else -> {
            mapOf(
                "har_nattevaak" to nattevaak.harNattevaak,
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
        "vet_ikke" == tilsynsordning.svar -> mapOf(
            "tilsynsordning_svar" to "vet_ikke",
            "svar" to tilsynsordning.vetIkke?.svar,
            "annet" to tilsynsordning.vetIkke?.annet
        )
        "nei" == tilsynsordning.svar -> mapOf(
            "tilsynsordning_svar" to "nei"
        )
        else -> null
    }

    private fun PdfRendererBuilder.medFonter() =
        useFont({ ByteArrayInputStream(REGULAR_FONT) }, "Source Sans Pro", 400, BaseRendererBuilder.FontStyle.NORMAL, false)
        .useFont({ ByteArrayInputStream(BOLD_FONT) }, "Source Sans Pro", 700, BaseRendererBuilder.FontStyle.NORMAL, false)
        .useFont({ ByteArrayInputStream(ITALIC_FONT) }, "Source Sans Pro", 400, BaseRendererBuilder.FontStyle.ITALIC, false)
}

private fun List<Organisasjon>.somMap() = map {
    val skalJobbeProsent = it.skalJobbeProsent?.avrundetMedEnDesimal()
    val jobberNormaltimer = it.jobberNormaltTimer
    val inntektstapProsent = skalJobbeProsent?.skalJobbeProsentTilInntektstap()
    val vetIkkeEkstrainfo = it.vetIkkeEkstrainfo

    mapOf<String,Any?>(
        "navn" to it.navn,
        "organisasjonsnummer" to it.formaterOrganisasjonsnummer(),
        "skal_jobbe" to it.skalJobbe,
        "skal_jobbe_prosent" to skalJobbeProsent?.formatertMedEnDesimal(),
        "inntektstap_prosent" to inntektstapProsent?.formatertMedEnDesimal(),
        "jobber_normaltimer" to jobberNormaltimer,
        "vet_ikke_ekstra_info" to vetIkkeEkstrainfo
    )
}

private fun List<Bosted>.somMapBosted(): List<Map<String, Any?>> {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.of("Europe/Oslo"))
    return map {
        mapOf<String,Any?>(
            "landnavn" to it.landnavn,
            "fraOgMed" to dateFormatter.format(it.fraOgMed),
            "tilOgMed" to dateFormatter.format(it.tilOgMed)
            )
    }
}

private fun List<Utenlandsopphold>.somMapUtenlandsopphold(): List<Map<String, Any?>> {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.of("Europe/Oslo"))
    return map {
        mapOf<String,Any?>(
            "landnavn" to it.landnavn,
            "fraOgMed" to dateFormatter.format(it.fraOgMed),
            "tilOgMed" to dateFormatter.format(it.tilOgMed)
            )
    }
}

private fun List<Ferieuttak>.somMapFerieuttak(): List<Map<String, Any?>> {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.of("Europe/Oslo"))
    return map {
        mapOf<String,Any?>(
            "fraOgMed" to dateFormatter.format(it.fraOgMed),
            "tilOgMed" to dateFormatter.format(it.tilOgMed)
            )
    }
}

private fun List<Organisasjon>.erAktuelleArbeidsgivere() = any { it.skalJobbeProsent != null }

private fun Soker.formatertNavn() = if (mellomnavn != null) "$fornavn $mellomnavn $etternavn" else "$fornavn $etternavn"
private fun String.sprakTilTekst() = when (this.toLowerCase()) {
    "nb" -> "bokmål"
    "nn" -> "nynorsk"
    else -> this
}
