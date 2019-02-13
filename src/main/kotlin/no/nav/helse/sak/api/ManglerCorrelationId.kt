package no.nav.helse.sak.api

import io.ktor.http.HttpHeaders

class ManglerCorrelationId : IllegalStateException("Mangler header ${HttpHeaders.XCorrelationId}")