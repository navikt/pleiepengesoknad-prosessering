package no.nav.helse.sak.v1

import no.nav.helse.sak.FagSystem
import no.nav.helse.sak.AktoerId
import no.nav.helse.sak.CorrelationId
import no.nav.helse.sak.SakId
import no.nav.helse.sak.Tema
import no.nav.helse.sak.gateway.SakGateway
import no.nav.helse.validering.Brudd
import no.nav.helse.validering.Valideringsfeil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("nav.SakV1Service")
private val GOSYS_FAGSYSTEM = FagSystem("GOSYS", "FS22")
private val OMSORG_TEMA = Tema("OMS")
private val ONLY_DIGITS = Regex("\\d+")

class SakV1Service(
    private val sakGateway: SakGateway
) {
    suspend fun opprettSak(
        melding: MeldingV1,
        metaData: MetadataV1) : SakId {

        logger.info(metaData.toString())

        validerMelding(melding)

        val request = OpprettSakRequestV1Factory.instance(
            aktoerId = AktoerId(value = melding.aktoerId),
            tema = OMSORG_TEMA,
            fagSystem = GOSYS_FAGSYSTEM
        )

        val response = sakGateway.opprettSak(
            request = request,
            correlationId = CorrelationId(
                value = metaData.correlationId
            )
        )

        return SakId(value = response.id)
    }

    private fun validerMelding(melding: MeldingV1) {
        val brudd = mutableListOf<Brudd>()
        if (!melding.aktoerId.matches(ONLY_DIGITS)) {
            brudd.add(Brudd("aktoer_id", error = "${melding.aktoerId} er ikke en gyldig AktørID. Kan kun være siffer."))
        }
        if (brudd.isNotEmpty()) {
            throw Valideringsfeil(brudd)
        }
    }
}