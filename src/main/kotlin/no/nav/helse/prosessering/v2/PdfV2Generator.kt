package no.nav.helse.prosessering.v2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.context.MapValueResolver
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import no.nav.helse.dusseldorf.ktor.core.fromResources
import no.nav.helse.felles.Næringstyper
import no.nav.helse.pleiepengerKonfiguert
import no.nav.helse.prosessering.v1.formaterOrganisasjonsnummer
import no.nav.helse.utils.DateUtils
import no.nav.helse.utils.norskDag
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.LovbestemtFerie
import no.nav.k9.søknad.felles.aktivitet.ArbeidAktivitet
import no.nav.k9.søknad.felles.aktivitet.Arbeidstaker
import no.nav.k9.søknad.felles.aktivitet.Frilanser
import no.nav.k9.søknad.felles.aktivitet.SelvstendigNæringsdrivende
import no.nav.k9.søknad.felles.aktivitet.VirksomhetType
import no.nav.k9.søknad.felles.personopplysninger.Bosteder
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold
import no.nav.k9.søknad.ytelse.psb.v1.Beredskap
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.Tilsynsordning
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

internal class PdfV2Generator {
    companion object {

        private const val ROOT = "handlebars"
        private const val SOKNAD = "soknadV2"

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
                VirksomhetType.valueOf(context)
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

        private fun loadImages() = mapOf(
            "Checkbox_off.png" to loadPng("Checkbox_off"),
            "Checkbox_on.png" to loadPng("Checkbox_on"),
            "Hjelp.png" to loadPng("Hjelp"),
            "Navlogo.png" to loadPng("Navlogo"),
            "Personikon.png" to loadPng("Personikon"),
            "Fritekst.png" to loadPng("Fritekst")
        )

        fun Søknad.generateSoknadOppsummeringPdf(): ByteArray {
            soknadTemplate.apply(
                Context
                    .newBuilder(this.somMap())
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

        private fun Søknad.somMap(): Map<String, Any?> {
            val ytelse = getYtelse<PleiepengerSyktBarn>()
            return mapOf(
                "søknadId" to søknadId.id,
                "mottattDag" to mottattDato.withZoneSameInstant(ZONE_ID).norskDag(),
                "mottattDato" to DATE_TIME_FORMATTER.format(mottattDato),
                "harMedsøker" to ytelse.søknadInfo.harMedsøker,
                "samtidigHjemme" to ytelse.søknadInfo.samtidigHjemme,
                "bekrefterPeriodeOver8Uker" to ytelse.søknadInfo.bekrefterPeriodeOver8Uker,
                "søker" to mapOf(
                    "norskIdentitetsnummer" to søker.norskIdentitetsnummer.verdi
                ),
                "barn" to mapOf(
                    "fødselsdato" to ytelse.barn.fødselsdato?.format(DATE_FORMATTER),
                    "norskIdentitetsnummer" to ytelse.barn.norskIdentitetsnummer.verdi
                ),
                "søknadperiode" to mapOf(
                    "fraOgMed" to DATE_FORMATTER.format(ytelse.søknadsperiode.fraOgMed),
                    "tilOgMed" to DATE_FORMATTER.format(ytelse.søknadsperiode.tilOgMed),
                    "virkedager" to DateUtils.antallVirkedager(
                        ytelse.søknadsperiode.fraOgMed,
                        ytelse.søknadsperiode.tilOgMed
                    )
                ),
                "opptjening" to ytelse.arbeidAktivitet.somMap(),
                "arbeidstid" to mapOf(
                    "harArbeidsgivere" to ytelse.arbeidstid.arbeidstakerList.isNotEmpty(),
                    "arbeidstakerList" to ytelse.arbeidstid.arbeidstakerList.somMap()
                ),
                "bosteder" to ytelse.bosteder.somMapBosted(),
                "samtykke" to mapOf(
                    "harForståttRettigheterOgPlikter" to ytelse.søknadInfo.harForståttRettigheterOgPlikter,
                    "harBekreftetOpplysninger" to ytelse.søknadInfo.harBekreftetOpplysninger
                ),
                "hjelp" to mapOf(
                    "harMedsøker" to ytelse.søknadInfo.harMedsøker,
                    "ingenArbeidsgivere" to ytelse.arbeidstid.arbeidstakerList.isEmpty(),
                    "tarUtLovbestemtFerie" to ytelse.lovbestemtFerie.perioder.isNotEmpty(),
                    "harTilsynsordning" to ytelse.tilsynsordning.perioder.isNotEmpty(),
                    "harNattevåk" to ytelse.nattevåk.perioder.isNotEmpty(),
                    "iBeredskap" to ytelse.beredskap.perioder.isNotEmpty()
                ),
                "tilsynsordning" to ytelse.tilsynsordning.somMap(),
                "nattevåk" to ytelse.nattevåk.somMap(),
                "beredskap" to ytelse.beredskap.somMap(),
                "utenlandsopphold" to ytelse.utenlandsopphold.somMap(),
                "lovbestemtFerie" to ytelse.lovbestemtFerie.somMap(),
                "samtykketOmsorgForBarnet" to ytelse.søknadInfo.samtykketOmsorgForBarnet,
                "skal_passe_pa_barnet_i_hele_perioden" to null, // TODO: 01/02/2021 Mangler på k9-format
                "beskrivelseAvOmsorgsrollen" to ytelse.søknadInfo.beskrivelseAvOmsorgsrollen,
                "relasjonTilBarnet" to ytelse.søknadInfo.relasjonTilBarnet,
                "barnRelasjonBeskrivelse" to null // TODO: 01/02/2021 Mangler i k9-format
            )
        }

        private fun no.nav.k9.søknad.ytelse.psb.v1.Nattevåk.somMap(): List<Map<String, String?>> = perioder.map {
            val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.of("Europe/Oslo"))
            mapOf(
                "fraOgMed" to dateFormatter.format(it.key.fraOgMed),
                "tilOgMed" to dateFormatter.format(it.key.tilOgMed),
                "tilleggsinformasjon" to it.value.tilleggsinformasjon
            )
        }

        private fun Tilsynsordning.somMap(): List<Map<String, Any>> = perioder.map {
            mapOf(
                "fraOgMed" to DATE_FORMATTER.format(it.key.fraOgMed),
                "tilOgMed" to DATE_FORMATTER.format(it.key.tilOgMed),
                "etablertTilsynTimerPerDag" to it.value.etablertTilsynTimerPerDag.toHours()
            )
        }

        private fun LovbestemtFerie.somMap(): List<Map<String, String>> = perioder.map {
            mapOf(
                "fraOgMed" to DATE_FORMATTER.format(it.fraOgMed),
                "tilOgMed" to DATE_FORMATTER.format(it.tilOgMed)
            )
        }

        private fun Utenlandsopphold.somMap(): List<Map<String, Any?>> = perioder.map {
            mapOf(
                "fraOgMed" to DATE_FORMATTER.format(it.key.fraOgMed),
                "tilOgMed" to DATE_FORMATTER.format(it.key.tilOgMed),
                "land" to it.value.land.landkode,
                "årsak" to it.value.årsak.name.toLowerCase().replace("_", " ").capitalize()
            )
        }

        private fun Beredskap.somMap(): List<Map<String, String?>> = perioder.map {
            mapOf(
                "fraOgMed" to DATE_FORMATTER.format(it.key.fraOgMed),
                "tilOgMed" to DATE_FORMATTER.format(it.key.tilOgMed),
                "tilleggsinformasjon" to it.value.tilleggsinformasjon
            )
        }

        private fun List<Arbeidstaker>.somMap(): List<Map<String, Any?>> = map {
            val arbeidstidInfo = it.arbeidstidInfo
            val jobberNormaltimer = arbeidstidInfo.jobberNormaltTimerPerDag

            mapOf(
                "organisasjonsnummer" to it.organisasjonsnummer.formaterOrganisasjonsnummer(),
                "jobberNormaltimer" to jobberNormaltimer.somTekst(),
                "faktiskArbeidstimer" to arbeidstidInfo.perioder.map { entry ->
                    val periode = entry.key
                    val arbeidstidPeriodeInfo = entry.value
                    mapOf(
                        "fraOgMed" to DATE_FORMATTER.format(periode.fraOgMed),
                        "tilOgMed" to DATE_FORMATTER.format(periode.tilOgMed),
                        "faktiskArbeidTimerPerDag" to arbeidstidPeriodeInfo.faktiskArbeidTimerPerDag.tilString()
                    )
                }
            )
        }

        private fun Bosteder.somMapBosted(): List<Map<String, String>> {
            return perioder.map {
                val periode = it.key
                val bostedPeriodeInfo = it.value

                mapOf(
                    "fraOgMed" to DATE_FORMATTER.format(periode.fraOgMed),
                    "tilOgMed" to DATE_FORMATTER.format(periode.tilOgMed),
                    "landkode" to bostedPeriodeInfo.land.landkode
                )
            }.sortedByDescending {
                LocalDate.parse(it["tilOgMed"], DATE_FORMATTER)
            }
        }

        private fun ArbeidAktivitet.somMap() = mapOf(
            "frilanser" to frilanser.somMap(),
            "selvstendignæringsdrivende" to selvstendigNæringsdrivende.map { it.somMap() }
        )

        private fun Frilanser.somMap() = mapOf(
            "startdato" to startdato,
            "jobberFortsattSomFrilanser" to jobberFortsattSomFrilans
        )

        private fun SelvstendigNæringsdrivende.somMap() = mapOf(
            "virksomhetsnavn" to virksomhetNavn,
            "organisasjonsnummer" to organisasjonsnummer.verdi,
            "yrkesAktivDato" to perioder.map { it.key.fraOgMed }.sorted().first().toString(),
            "perioder" to perioder.map { entry ->
                val periode = entry.key
                val næringsdrivendePeriodeInfo = entry.value

                mapOf(
                    "fraOgMed" to DATE_FORMATTER.format(periode.fraOgMed),
                    "tilOgMed" to DATE_FORMATTER.format(periode.tilOgMed),
                    "periodeinfo" to næringsdrivendePeriodeInfo.somMap()
                )
            }
        )

        private fun SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo.somMap() = mapOf(
            "bruttoInntekt" to bruttoInntekt,
            "regnskapsførerNavn" to regnskapsførerNavn,
            "regnskapsførerTlf" to regnskapsførerTlf,
            "endringDato" to endringDato,
            "erNyoppstartet" to erNyoppstartet,
            "erVarigEndring" to erVarigEndring,
            "endringBegrunnelse" to endringBegrunnelse,
            "registrertIUtlandet" to registrertIUtlandet,
            "landkode" to landkode.landkode,
            "virksomhetstyper" to virksomhetstyper.map { it.navn }
        )
    }
}

private fun Duration.tilString(): String = when (this.toMinutesPart()) {
    0 -> "${this.toHoursPart()} timer"
    else -> "${this.toHoursPart()} timer og ${this.toMinutesPart()} minutter"
}
