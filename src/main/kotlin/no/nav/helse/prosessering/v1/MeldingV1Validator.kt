package no.nav.helse.prosessering.v1

import no.nav.helse.dusseldorf.ktor.core.*
import java.time.format.DateTimeFormatter

private const val MIN_GRAD = 20
private const val MAX_GRAD = 100

internal fun MeldingV1.validate() {
    val violations = mutableSetOf<Violation>()
    if (vedlegg.isEmpty() && vedleggUrls.isEmpty()) {
        violations.add(
            Violation(
                parameterName = "vedlegg",
                parameterType = ParameterType.ENTITY,
                reason = "Det må sendes minst et vedlegg eller en vedlegg URL.",
                invalidValue = vedlegg
            )
        )
        violations.add(
            Violation(
                parameterName = "vedlegg_urls",
                parameterType = ParameterType.ENTITY,
                reason = "Det må sendes minst et vedlegg eller en vedlegg URL.",
                invalidValue = vedleggUrls
            )
        )
    }

    // TODO: Validere innhold av listen "vedlegg"

    if (!soker.fodselsnummer.erGyldigFodselsnummer()) {
        violations.add(
            Violation(
                parameterName = "soker.fodselsnummer",
                parameterType = ParameterType.ENTITY,
                reason = "Ikke gyldig fødselsnummer.",
                invalidValue = soker.fodselsnummer
            )
        )
    }

    if (barn.fodselsnummer != null && !barn.fodselsnummer.erGyldigFodselsnummer()) {
        violations.add(
            Violation(
                parameterName = "barn.fodselsnummer",
                parameterType = ParameterType.ENTITY,
                reason = "Ikke gyldig fødselsnummer.",
                invalidValue = barn.fodselsnummer
            )
        )
    }

    if (barn.alternativId != null && !barn.alternativId.erKunSiffer()) {
        violations.add(
            Violation(
                parameterName = "barn.alternativ_id",
                parameterType = ParameterType.ENTITY,
                reason = "Ikke gyldig alternativ id. Kan kun inneholde tall.",
                invalidValue = barn.alternativId
            )
        )
    }

    arbeidsgivere.organisasjoner.mapIndexed { index, organisasjon ->
        if (!organisasjon.organisasjonsnummer.erGyldigOrganisasjonsnummer()) {
            violations.add(
                Violation(
                    parameterName = "arbeidsgivere.organisasjoner[$index].organisasjonsnummer",
                    parameterType = ParameterType.ENTITY,
                    reason = "Ikke gyldig organisasjonsnummer.",
                    invalidValue = organisasjon.organisasjonsnummer
                )
            )
        }
    }

    // Grad
    if (grad < MIN_GRAD || grad > MAX_GRAD) {
        violations.add(
            Violation(
                parameterName = "grad",
                parameterType = ParameterType.ENTITY,
                reason = "Grad må være mellom $MIN_GRAD og $MAX_GRAD.",
                invalidValue = grad

            ))
    }

    if (!tilOgMed.isAfter(fraOgMed)) {
        violations.add(
            Violation(
                parameterName = "fra_og_med",
                parameterType = ParameterType.ENTITY,
                reason = "Fra og med må være før til og med.",
                invalidValue = DateTimeFormatter.ISO_DATE.format(fraOgMed)
            )
        )
        violations.add(
            Violation(
                parameterName = "til_og_med",
                parameterType = ParameterType.ENTITY,
                reason = "Til og med må være etter fra og med.",
                invalidValue = DateTimeFormatter.ISO_DATE.format(tilOgMed)
            )
        )
    }

    if (violations.isNotEmpty()) {
        throw Throwblem(ValidationProblemDetails(violations))
    }
}