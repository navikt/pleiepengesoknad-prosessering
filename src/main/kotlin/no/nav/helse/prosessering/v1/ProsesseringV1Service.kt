package no.nav.helse.prosessering.v1

import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerService
import no.nav.helse.aktoer.Fodselsnummer
import no.nav.helse.dokument.DokumentService
import no.nav.helse.dusseldorf.ktor.core.*
import no.nav.helse.gosys.GosysService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val logger: Logger = LoggerFactory.getLogger("nav.ProsesseringV1Service")

class ProsesseringV1Service(
    private val gosysService: GosysService,
    private val aktoerService: AktoerService,
    private val pdfV1Generator: PdfV1Generator,
    private val dokumentService: DokumentService
) {
    suspend fun leggSoknadTilProsessering(
        melding: MeldingV1,
        metadata: MetadataV1
    ) {
        logger.info(metadata.toString())

        validerMelding(melding)

        reportMetrics(melding = melding)

        val correlationId = CorrelationId(metadata.correlationId)

        logger.trace("Henter AktørID for søkeren.")
        val sokerAktoerId = aktoerService.getAktorId(
            fnr = Fodselsnummer(melding.soker.fodselsnummer),
            correlationId = correlationId
        )

        logger.info("Søkerens AktørID = $sokerAktoerId")

        logger.trace("Henter AktørID for barnet.")
        val barnAktoerId = if (melding.barn.fodselsnummer != null) {
            try {
                aktoerService.getAktorId(
                    fnr = Fodselsnummer(melding.barn.fodselsnummer),
                    correlationId = correlationId
                )
            } catch (cause: Throwable) {
                logger.warn("Feil ved oppslag på Aktør ID basert på barnets fødselsnummer. Kan være at det ikke er registrert i Aktørregisteret enda. ${cause.message}")
                null
            }
        } else null

        logger.info("Barnets AktørID = $barnAktoerId")

        logger.trace("Genererer Oppsummerings-PDF av søknaden.")

        val soknadOppsummeringPdf = pdfV1Generator.generateSoknadOppsummeringPdf(melding)

        logger.trace("Generering av Oppsummerings-PDF OK.")
        logger.trace("Mellomlagrer Oppsummerings-PDF.")

        val soknadOppsummeringPdfUrl = dokumentService.lagreSoknadsOppsummeringPdf(
            pdf = soknadOppsummeringPdf,
            correlationId = correlationId,
            aktoerId = sokerAktoerId
        )

        logger.trace("Mellomlagring av Oppsummerings-PDF OK")

        logger.trace("Mellomlagrer Oppsummerings-JSON")

        val soknadJsonUrl = dokumentService.lagreSoknadsMelding(
            melding = melding,
            aktoerId = sokerAktoerId,
            correlationId = correlationId
        )

        logger.trace("Mellomlagrer Oppsummerings-JSON OK.")


        val komplettDokumentUrls = mutableListOf(
            listOf(
                soknadOppsummeringPdfUrl,
                soknadJsonUrl
            )
        )

        if (melding.vedleggUrls.isNotEmpty()) {
            logger.trace("Legger til ${melding.vedleggUrls.size} vedlegg URL's fra meldingen som dokument.")
            melding.vedleggUrls.forEach { it -> komplettDokumentUrls.add(listOf(it))}
        }
        if (melding.vedlegg.isNotEmpty()) {
            logger.trace("Meldingen inneholder ${melding.vedlegg.size} vedlegg som må mellomlagres før søknaden legges til prosessering.")
            val lagredeVedleggUrls = dokumentService.lagreVedlegg(
                vedlegg = melding.vedlegg,
                correlationId = correlationId,
                aktoerId = sokerAktoerId
            )
            logger.trace("Mellomlagring OK, legger til URL's som dokument.")
            lagredeVedleggUrls.forEach { it -> komplettDokumentUrls.add(listOf(it))}
        }

        logger.trace("Totalt ${komplettDokumentUrls.size} dokumentbolker.")

        logger.trace("Oppretter oppgave i Gosys")

        val komplettDokumentUrlsList = komplettDokumentUrls.toList()

        gosysService.opprett(
            sokerAktoerId = sokerAktoerId,
            barnAktoerId = barnAktoerId,
            mottatt = melding.mottatt,
            dokumenter = komplettDokumentUrlsList,
            correlationId = correlationId
        )

        logger.trace("Oppgave i Gosys opprettet OK")

        logger.trace("Sletter dokumenter.")

        try { dokumentService.slettDokumeter(
            urlBolks = komplettDokumentUrlsList,
            aktoerId = sokerAktoerId,
            correlationId = correlationId
        )} catch (cause: Throwable) {
            logger.warn("Feil ved sletting av dokumenter etter oppgave opprettet i Gosys", cause)
        }
    }

    private val periodeSoknadGjelderIUkerHistogram = Histogram.build()
        .buckets(0.00, 1.00, 4.00, 8.00, 12.00, 16.00, 20.00, 24.00, 28.00, 32.00, 36.00, 40.00, 44.00, 48.00, 52.00)
        .name("antall_uker_soknaden_gjelder_histogram")
        .help("Antall uker søknaden gjelder")
        .register()

    private val valgteArbeidsgivereHistogram = Histogram.build()
        .buckets(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
        .name("antall_valgte_arbeidsgivere_histogram")
        .help("Antall arbeidsgivere valgt i søknadene")
        .register()

    private val opplastedeVedleggHistogram = Histogram.build()
        .buckets(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
        .name("antall_oppplastede_vedlegg_histogram")
        .help("Antall vedlegg lastet opp i søknader")
        .register()

    private val idTypePaaBarnCounter = Counter.build()
        .name("id_type_paa_barn_counter")
        .help("Teller for hva slags ID-Type som er benyttet for å identifisere barnet")
        .labelNames("id_type")
        .register()

    private fun reportMetrics(melding : MeldingV1) {
        valgteArbeidsgivereHistogram.observe(melding.arbeidsgivere.organisasjoner.size.toDouble())
        opplastedeVedleggHistogram.observe( (melding.vedlegg.size + melding.vedleggUrls.size).toDouble() )
        idTypePaaBarnCounter.labels(if (melding.barn.fodselsnummer != null) "fodselsnummer" else "alternativ_id").inc()
        periodeSoknadGjelderIUkerHistogram.observe(ChronoUnit.WEEKS.between(melding.fraOgMed, melding.tilOgMed).toDouble())
    }

    private fun validerMelding(melding : MeldingV1) {
        val violations = mutableSetOf<Violation>()
        if (melding.vedlegg.isEmpty() && melding.vedleggUrls.isEmpty()) {
            violations.add(Violation(
                parameterName = "vedlegg",
                parameterType = ParameterType.ENTITY,
                reason = "Det må sendes minst et vedlegg eller en vedlegg URL.",
                invalidValue = melding.vedlegg
            ))
            violations.add(Violation(
                parameterName = "vedlegg_urls",
                parameterType = ParameterType.ENTITY,
                reason = "Det må sendes minst et vedlegg eller en vedlegg URL.",
                invalidValue = melding.vedleggUrls
            ))
        }

        // TODO: Validere innhold av listen "vedlegg"

        if (!melding.soker.fodselsnummer.erGyldigFodselsnummer()) {
            violations.add(Violation(
                parameterName = "soker.fodselsnummer",
                parameterType = ParameterType.ENTITY,
                reason = "Ikke gyldig fødselsnummer.",
                invalidValue = melding.soker.fodselsnummer
            ))
        }

        if (melding.barn.fodselsnummer != null && !melding.barn.fodselsnummer.erGyldigFodselsnummer()) {
            violations.add(Violation(
                parameterName = "barn.fodselsnummer",
                parameterType = ParameterType.ENTITY,
                reason = "Ikke gyldig fødselsnummer.",
                invalidValue = melding.barn.fodselsnummer
            ))
        }

        if (melding.barn.alternativId != null && !melding.barn.alternativId.erKunSiffer()) {
            violations.add(Violation(
                parameterName = "barn.alternativ_id",
                parameterType = ParameterType.ENTITY,
                reason = "Ikke gyldig alternativ id. Kan kun inneholde tall.",
                invalidValue = melding.barn.alternativId
            ))
        }

        melding.arbeidsgivere.organisasjoner.mapIndexed { index, organisasjon ->
            if (!organisasjon.organisasjonsnummer.erGyldigOrganisasjonsnummer()) {
                violations.add(Violation(
                    parameterName = "arbeidsgivere.organisasjoner[$index].organisasjonsnummer",
                    parameterType = ParameterType.ENTITY,
                    reason = "Ikke gyldig organisasjonsnummer.",
                    invalidValue = organisasjon.organisasjonsnummer
                ))
            }
        }

        if (!melding.tilOgMed.isAfter(melding.fraOgMed)) {
            violations.add(Violation(
                parameterName = "fra_og_med",
                parameterType = ParameterType.ENTITY,
                reason = "Fra og med må være før til og med.",
                invalidValue = DateTimeFormatter.ISO_DATE.format(melding.fraOgMed)
            ))
            violations.add(Violation(
                parameterName = "til_og_med",
                parameterType = ParameterType.ENTITY,
                reason = "Til og med må være etter fra og med.",
                invalidValue = DateTimeFormatter.ISO_DATE.format(melding.tilOgMed)
            ))
        }

        if (violations.isNotEmpty()) {
            throw Throwblem(ValidationProblemDetails(violations))
        }
    }
}
