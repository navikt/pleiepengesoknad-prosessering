package no.nav.helse.aktoer

data class Fodselsnummer(private val value: String) : NorskIdent {
    override fun getValue() = value
}