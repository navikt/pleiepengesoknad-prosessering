package no.nav.helse.prosessering.v1

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.*
import no.nav.helse.barn.BarnOppslag
import no.nav.helse.dokument.DokumentService
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.SoknadId
import no.nav.helse.tpsproxy.Ident
import no.nav.helse.tpsproxy.TpsNavn
import org.slf4j.LoggerFactory

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
        val barnAktoerId: AktoerId? = when {
            melding.barn.aktoerId.isNullOrBlank() -> hentBarnetsAktoerId(barn = melding.barn, correlationId = correlationId)
            else -> AktoerId(melding.barn.aktoerId)
        }
        logger.info("Barnets AktørID = $barnAktoerId")

        val barnetsIdent: NorskIdent? = when {
            barnAktoerId != null -> aktoerService.getIdent(barnAktoerId.id, correlationId = correlationId)
            else -> null
        }

        val barnetsNavn: String? = slaaOppBarnetsNavn(melding.barn, barnetsIdent = barnetsIdent, correlationId = correlationId)

        logger.trace("Genererer Oppsummerings-PDF av søknaden.")

        val soknadOppsummeringPdf = pdfV1Generator.generateSoknadOppsummeringPdf(melding, barnetsIdent, barnetsNavn)

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


        val preprossesertMeldingV1 = PreprossesertMeldingV1(
            dokumentUrls = komplettDokumentUrls.toList(),
            melding = melding,
            sokerAktoerId = sokerAktoerId,
            barnAktoerId = barnAktoerId,
            barnetsNavn = barnetsNavn,
            barnetsNorskeIdent = barnetsIdent
        )
        melding.reportMetrics()
        preprossesertMeldingV1.reportMetrics()
        return preprossesertMeldingV1
    }

    /**
     * Slår opp barnets navn, gitt enten alternativId, fødselsNummer eller aktørId.
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
        return "${tpsNavn.fornavn} ${tpsNavn.mellomnavn} ${tpsNavn.etternavn}"
    }

    private suspend fun hentBarnetsAktoerId(
        barn: Barn,
        correlationId: CorrelationId
    ): AktoerId? {
        return try {
            when {
                !barn.fodselsnummer.isNullOrBlank() -> aktoerService.getAktorId(
                    ident = Fodselsnummer(barn.fodselsnummer),
                    correlationId = correlationId
                )
                !barn.alternativId.isNullOrBlank() -> aktoerService.getAktorId(
                    ident = AlternativId(barn.alternativId),
                    correlationId = correlationId
                )
                else -> null
            }
        } catch (cause: Throwable) {
            logger.warn("Feil ved oppslag på Aktør ID basert på barnets fødselsnummer. Kan være at det ikke er registrert i Aktørregisteret enda. ${cause.message}")
            null
        }
    }
}