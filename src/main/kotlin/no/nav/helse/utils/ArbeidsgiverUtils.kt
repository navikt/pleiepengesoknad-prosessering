package no.nav.helse.prosessering.v1

import no.nav.helse.felles.Organisasjon
import no.nav.k9.søknad.felles.type.Organisasjonsnummer
import java.math.BigDecimal
import java.math.RoundingMode

internal fun Double.avrundetMedEnDesimal() = BigDecimal(this).setScale(1, RoundingMode.HALF_UP).toDouble()
internal fun Double.formatertMedEnDesimal() = String.format("%.1f", this)
internal fun Organisasjon.formaterOrganisasjonsnummer() = if (organisasjonsnummer.length == 9) "${organisasjonsnummer.substring(0,3)} ${organisasjonsnummer.substring(3,6)} ${organisasjonsnummer.substring(6)}" else organisasjonsnummer
internal fun Organisasjonsnummer.formaterOrganisasjonsnummer() = if (verdi.length == 9) "${verdi.substring(0,3)} ${verdi.substring(3,6)} ${verdi.substring(6)}" else verdi
internal fun Double.skalJobbeProsentTilInntektstap() = 100.0 - this
