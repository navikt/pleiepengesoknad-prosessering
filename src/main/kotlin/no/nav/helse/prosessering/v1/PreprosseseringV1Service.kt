package no.nav.helse.prosessering.v1

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.aktoer.AktoerService
import no.nav.helse.aktoer.Fodselsnummer
import no.nav.helse.aktoer.NorskIdent
import no.nav.helse.barn.BarnOppslag
import no.nav.helse.dokument.DokumentService
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.SoknadId
import no.nav.helse.tpsproxy.Ident
import no.nav.helse.tpsproxy.TpsNavn
import org.slf4j.LoggerFactory

private const val ATTRIBUTT_QUERY_NAVN = "a"

internal class PreprosseseringV1Service(
    private val aktoerService: AktoerService,
    private val pdfV1Generator: PdfV1Generator,
    private val dokumentService: DokumentService,
    private val barnOppslag: BarnOppslag
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(PreprosseseringV1Service::class.java)
    }

    internal suspend fun preprosseser(
        melding: MeldingV1,
        metadata: Metadata
    ): PreprossesertMeldingV1 {
        val soknadId = SoknadId(melding.soknadId)
        logger.info("Preprosseserer $soknadId")

        val correlationId = CorrelationId(metadata.correlationId)

        val sokerAktoerId = AktoerId(melding.soker.aktoerId)

        logger.info("Søkerens AktørID = $sokerAktoerId")

        logger.trace("Henter AktørID for barnet.")
        val barnAktoerId = hentBarnetsAktoerId(barn = melding.barn, correlationId = correlationId)
        logger.info("Barnets AktørID = $barnAktoerId")

        val barnetsNavn: String = slaaOppBarnetsNavn(melding.barn, correlationId = correlationId)

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
            melding.vedleggUrls.forEach { komplettDokumentUrls.add(listOf(it)) }
        }

        logger.trace("Totalt ${komplettDokumentUrls.size} dokumentbolker.")

        melding.reportMetrics()

        return PreprossesertMeldingV1(
            dokumentUrls = komplettDokumentUrls.toList(),
            melding = melding,
            sokerAktoerId = sokerAktoerId,
            barnAktoerId = barnAktoerId,
            barnetsNavn = barnetsNavn
        )
    }

    /**
     * Slår opp barnets navn, gitt enten alternativId, fødselsNummer eller aktørId.
     */
    private suspend fun slaaOppBarnetsNavn(
        barn: Barn,
        correlationId: CorrelationId
    ): String {

        return when {
            // Dersom barnet har navn, returner navnet.
            !barn.navn.isNullOrBlank() -> barn.navn

            // Ellers, hvis barnet har et fødselsNummer ...
            !barn.fodselsnummer.isNullOrBlank() -> {
                // Slå opp på i barneOppslag med fødselsnummer ...
                logger.info("Henter barnets navn gitt fødselsnummer ...")
                getFullNavn(ident = barn.fodselsnummer, correlationId = correlationId)
            }
            // Ellers, hvis barnet har et alternativId ...
            !barn.alternativId.isNullOrBlank() -> {
                // Slå opp på i barneOppslag med alternativId ...
                logger.info("Henter barnets navn gitt alternativId ...")
                getFullNavn(ident = barn.alternativId, correlationId = correlationId)
            }
            // Ellers hvis
            !barn.aktoerId.isNullOrBlank() -> {
                logger.info("Henter barnets navn gitt aktørId ...")
                val fodselsnummer: NorskIdent = aktoerService.getIdent(barn.aktoerId, correlationId = correlationId)
                getFullNavn(ident = fodselsnummer.getValue(), correlationId = correlationId)
            }
            else -> ""
        }
    }

    private suspend fun getFullNavn(ident: String, correlationId: CorrelationId): String {
        val tpsNavn: TpsNavn = barnOppslag.navn(Ident(ident), correlationId)
        return "${tpsNavn.fornavn} ${tpsNavn.mellomnavn} ${tpsNavn.etternavn}"
    }

    private suspend fun hentBarnetsAktoerId(
        barn: Barn,
        correlationId: CorrelationId
    ): AktoerId? {
        return if (barn.fodselsnummer != null) {
            try {
                aktoerService.getAktorId(
                    fnr = Fodselsnummer(barn.fodselsnummer),
                    correlationId = correlationId
                )
            } catch (cause: Throwable) {
                logger.warn("Feil ved oppslag på Aktør ID basert på barnets fødselsnummer. Kan være at det ikke er registrert i Aktørregisteret enda. ${cause.message}")
                null
            }
        } else null
    }
}