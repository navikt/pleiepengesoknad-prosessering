package no.nav.helse.prosessering

import java.util.*

data class SoknadId(val id: String) {
    companion object {
        fun generate() = SoknadId(UUID.randomUUID().toString())
    }
}