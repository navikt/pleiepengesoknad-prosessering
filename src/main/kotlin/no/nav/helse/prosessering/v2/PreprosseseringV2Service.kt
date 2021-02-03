package no.nav.helse.prosessering.v2

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.aktoer.AktoerService
import no.nav.helse.aktoer.NorskIdent
import no.nav.helse.aktoer.tilNorskIdent
import no.nav.helse.barn.BarnOppslag
import no.nav.helse.dokument.DokumentService
import no.nav.helse.felles.Barn
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.SoknadId
import no.nav.helse.prosessering.v1.reportMetrics
import no.nav.helse.prosessering.v2.PdfV2Generator.Companion.generateSoknadOppsummeringPdf
import no.nav.helse.tpsproxy.Ident
import no.nav.helse.tpsproxy.TpsNavn
import org.slf4j.LoggerFactory

internal class PreprosseseringV2Service(
    private val aktoerService: AktoerService,
    private val pdfGenerator: PdfV2Generator,
    private val dokumentService: DokumentService,
    private val barnOppslag: BarnOppslag
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(PreprosseseringV2Service::class.java)
    }

    internal suspend fun preprosseser(
        melding: MeldingV2,
        metadata: Metadata
    ): PreprossesertMeldingV2 {
        val soknadId = SoknadId(melding.søknad.søknadId.id)
        logger.info("Preprosseserer $soknadId")

        val correlationId = CorrelationId(metadata.correlationId)

        val sokerAktoerId = AktoerId( "FAKE"/*melding.søknad.søker.aktørId*/) // TODO: 29/01/2021 Trenger aktørId på søker

        logger.trace("Genererer Oppsummerings-PDF av søknaden.")

        val soknadOppsummeringPdf = melding.generateSoknadOppsummeringPdf()

        logger.trace("Generering av Oppsummerings-PDF OK.")
        logger.trace("Mellomlagrer Oppsummerings-PDF.")

        val soknadOppsummeringPdfUrl = dokumentService.lagreSoknadsOppsummeringPdf(
            pdf = soknadOppsummeringPdf,
            correlationId = correlationId,
            aktoerId = sokerAktoerId,
            dokumentbeskrivelse = "Søknad om pleiepenger"
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
            melding.vedleggUrls.forEach { komplettDokumentUrls.add(listOf(it)) }
        }

        logger.trace("Totalt ${komplettDokumentUrls.size} dokumentbolker.")

        val preprossesertMelding = PreprossesertMeldingV2(
            dokumentUrls = komplettDokumentUrls.toList(),
            søknad = melding.søknad,
            interInfo = melding.interInfo
        )

        preprossesertMelding.reportMetrics()
        return preprossesertMelding
    }

    /**
     * Slår opp barnets navn, fødselsNummer eller aktørId.
     */
    private suspend fun slaaOppBarnetsNavn(
        barn: Barn,
        correlationId: CorrelationId,
        barnetsIdent: NorskIdent?
    ): String? {

        return when {
            // Dersom barnet har navn, returner navnet.
            !barn.navn.isNullOrBlank() -> barn.navn

            // Dersom barnet har et norsk ident...
            barnetsIdent != null -> {
                // Slå opp på i barneOppslag med barnets ident ...
                logger.info("Henter barnets navn gitt fødselsnummer ...")
                return try {
                    getFullNavn(ident = barnetsIdent.getValue(), correlationId = correlationId)
                } catch (e: Exception) {
                    logger.warn("Oppslag for barnets navn feilet. Prosesserer melding uten barnets navn.")
                    null
                }
            }

            // Ellers returner null
            else -> {
                logger.warn("Kunne ikke finne barnets navn!")
                null
            }
        }
    }

    private suspend fun getFullNavn(ident: String, correlationId: CorrelationId): String {
        val tpsNavn: TpsNavn = barnOppslag.navn(Ident(ident), correlationId)
        return when {
            tpsNavn.mellomnavn.isNullOrBlank() -> "${tpsNavn.fornavn} ${tpsNavn.etternavn}"
            else -> "${tpsNavn.fornavn} ${tpsNavn.mellomnavn} ${tpsNavn.etternavn}"
        }
    }
}
