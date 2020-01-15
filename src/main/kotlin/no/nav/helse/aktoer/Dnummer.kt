package no.nav.helse.aktoer

data class Dnummer(private val value: String) : NorskIdent {
    override fun getValue() = value
}