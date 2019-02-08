package no.nav.helse.journalforing.v1

import io.ktor.http.ContentType
import no.nav.helse.journalforing.Kanal
import no.nav.helse.journalforing.AktoerId
import no.nav.helse.journalforing.SoknadId
import no.nav.helse.journalforing.Tema
import no.nav.helse.journalforing.gateway.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.IllegalStateException

private const val AKTOR_ID_KEY = "aktoer"
private const val IDENT_KEY = "ident"
private const val PERSON_KEY = "person"

private val PDF_CONTENT_TYPE = ContentType("application","pdf")
private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

object JournalPostRequestV1Factory {
    internal fun instance(
        mottaker: AktoerId,
        tema: Tema,
        kanal: Kanal,
        soknadId: SoknadId,
        dokumenter: List<DokumentV1>,
        mottatt: ZonedDateTime) : JournalPostRequest {

        if (dokumenter.isEmpty()) {
            throw IllegalStateException("Det må sendes minst ett dokument")
        }

        val forsendelseInformasjon = ForsendelseInformasjon(
            bruker = lagBrukerStruktur(aktorId = mottaker),
            tema = tema.value,
            kanalReferanseId = soknadId.value,
            forsendelseMottatt = DATE_TIME_FORMATTER.format(
                ensureUtc(
                    dateTime = mottatt
                )
            ),
            forsendelseInnsendt = DATE_TIME_FORMATTER.format(LocalDateTime.now(ZoneOffset.UTC)),
            mottaksKanal = kanal.value
        )


        var hovedDokument : Dokument? = null
        val vedlegg = mutableListOf<Dokument>()

        dokumenter.forEach { dokument ->
            if (hovedDokument == null) {
                hovedDokument = mapDokument(dokument)
            } else {
                vedlegg.add(mapDokument(dokument))
            }
        }

        return JournalPostRequest(
            forsokEndeligJF = true,
            forsendelseInformasjon = forsendelseInformasjon,
            dokumentInfoHoveddokument = hovedDokument!!,
            dokumentInfoVedlegg = vedlegg
        )
    }

    private fun ensureUtc(dateTime: ZonedDateTime): ZonedDateTime {
        return ZonedDateTime.ofInstant(dateTime.toInstant(), ZoneOffset.UTC)
    }

    private fun lagBrukerStruktur(aktorId: AktoerId): Map<String, Map<String, Map<String, String>>> {
        return mapOf(
            Pair(
                AKTOR_ID_KEY, mapOf(
                Pair(
                    PERSON_KEY, mapOf(
                    Pair(IDENT_KEY, aktorId.value)
                ))
            )
        ))
    }

    private fun mapDokument(dokument : DokumentV1) : Dokument {
        val arkivFilType = getArkivFilType(dokument)

        return Dokument(
            dokument.tittel,
            dokumentVariant = listOf(
                DokumentVariant(
                    arkivFilType = arkivFilType,
                    variantFormat = getVariantFormat(
                        arkivFilType
                    ),
                    dokument = dokument.innhold
                )
            )
        )
    }

    private fun getArkivFilType(dokument: DokumentV1) : ArkivFilType {
        if (PDF_CONTENT_TYPE == dokument.contentTypeObject) return ArkivFilType.PDFA
        throw IllegalStateException("Ikke støttet Content-Type '${dokument.contentType}'")
    }

    private fun getVariantFormat(arkivFilType: ArkivFilType) : VariantFormat {
        return if (arkivFilType.equals(ArkivFilType.PDFA)) VariantFormat.ARKIV else VariantFormat.ORIGINAL
    }
}