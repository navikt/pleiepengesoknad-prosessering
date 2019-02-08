package no.nav.helse.journalforing.v1

import io.ktor.http.ContentType

data class DokumentV1(
    val tittel : String,
    val innhold : ByteArray,
    val contentType : String
) {

    val contentTypeObject : ContentType

    init {
        val split = contentType.split("/")
        contentTypeObject = ContentType(split[0],split[1])
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DokumentV1

        if (tittel != other.tittel) return false
        if (!innhold.contentEquals(other.innhold)) return false
        if (contentType != other.contentType) return false
        if (contentTypeObject != other.contentTypeObject) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tittel.hashCode()
        result = 31 * result + innhold.contentHashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + contentTypeObject.hashCode()
        return result
    }
}