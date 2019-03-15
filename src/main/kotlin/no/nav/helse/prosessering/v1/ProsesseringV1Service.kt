package no.nav.helse.prosessering.v1

import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerService
import no.nav.helse.aktoer.Fodselsnummer
import no.nav.helse.dokument.DokumentService
import no.nav.helse.gosys.GosysService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Period

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

        // TODO: Validere melding

        reportMetrics(melding = melding)

        val correlationId = CorrelationId(metadata.correlationId)

        logger.trace("Henter AktørID for søkeren.")
        val sokerAktoerId = aktoerService.getAktorId(
            fnr = Fodselsnummer(melding.soker.fodselsnummer),
            correlationId = correlationId
        )

        logger.trace("Søkerens AktørID = $sokerAktoerId")

        logger.trace("Henter AktørID for barnet.")
        val barnAktoerId = if (melding.barn.fodselsnummer != null) {
            aktoerService.getAktorId(
                fnr = Fodselsnummer(melding.barn.fodselsnummer),
                correlationId = correlationId
            )
        } else null // TODO: Håndter feil her som null

        logger.trace("Barnets AktørID = $barnAktoerId")

        logger.trace("Genererer Oppsummerings-PDF av søknaden.")

        val soknadOppsummeringPdf = pdfV1Generator.generateSoknadOppsummeringPdf(melding)

        logger.trace("Generering av Oppsummerings-PDF OK.")
        logger.trace("Mellomlagrer Oppsummerings-PDF.")

        val soknadOppsummeringUrl = dokumentService.lagreSoknadsOppsummeringPdf(
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
            soknadOppsummeringUrl,
            soknadJsonUrl
        )
        if (melding.vedleggUrls.isNotEmpty()) {
            logger.trace("Legger til ${melding.vedleggUrls.size} vedlegg URL's fra meldingen som dokument.")
            komplettDokumentUrls.addAll(melding.vedleggUrls)
        }
        if (melding.vedlegg.isNotEmpty()) {
            logger.trace("Meldingen inneholder ${melding.vedlegg.size} vedlegg som må mellomlagres før søknaden legges til prosessering.")
            val lagredeVedleggUrls = dokumentService.lagreVedlegg(
                vedlegg = melding.vedlegg,
                correlationId = correlationId,
                aktoerId = sokerAktoerId
            )
            logger.trace("Mellomlagring OK, legger til URL's som dokument.")
            komplettDokumentUrls.addAll(lagredeVedleggUrls)
        }

        logger.trace("Totalt ${komplettDokumentUrls.size} dokumenter")
        logger.trace(komplettDokumentUrls.joinToString { it.toString() })

        logger.trace("Oppretter oppgave i Gosys")

        gosysService.opprett(
            sokerAktoerId = sokerAktoerId,
            barnAktoerId = barnAktoerId,
            mottatt = melding.mottatt,
            dokumenter = komplettDokumentUrls.toList(),
            correlationId = correlationId
        )

        logger.trace("Oppgave i Gosys opprettet OK")
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
        periodeSoknadGjelderIUkerHistogram.observe(Period.between(melding.fraOgMed, melding.tilOgMed).toWeeks())
    }
}

private fun Period.toWeeks(): Double = days.div(7).toDouble()