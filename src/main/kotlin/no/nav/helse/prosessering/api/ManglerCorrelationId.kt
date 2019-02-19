package no.nav.helse.prosessering.api

import io.ktor.http.HttpHeaders

class ManglerCorrelationId : IllegalStateException("Mangler header ${HttpHeaders.XCorrelationId}")