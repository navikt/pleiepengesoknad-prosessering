package no.nav.helse.journalforing.gateway

import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("nav.JournalPostResponse")

data class JournalPostResponse(
    val journalpostId: String,
    val journalTilstand: String,
    val dokumentIdListe: List<String>?
)

enum class JournalTilstand {
    ENDELIG_JOURNALFOERT,
    MIDLERTIDIG_JOURNALFOERT,
    UKJENT
}

fun journalTilstandFraString(stringValue: String) : JournalTilstand {
    JournalTilstand.values().forEach { journalTilstand ->
        if (journalTilstand.name.equals(stringValue, false)) {
            return journalTilstand
        }
    }
    logger.error("Mottok uventet response for attributet 'journalTilstand' Etter opprettelse av JournalPost. Mottok verdien '$stringValue'")
    return JournalTilstand.UKJENT
}