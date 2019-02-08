package no.nav.helse.validering

import java.lang.RuntimeException

class Valideringsfeil(val brudd: List<Brudd>) : RuntimeException("Ugyldig request")