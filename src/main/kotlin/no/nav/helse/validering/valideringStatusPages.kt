package no.nav.helse.validering

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.call
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import no.nav.helse.DefaultError
import java.net.URI

fun StatusPages.Configuration.valideringStatusPages() {

    val invalidParametersType = URI.create("/errors/invalid-parameters")
    val invalidParametersTitle = "Requesten inneholder ugyldige parametere."
    val invalidJsonType = URI.create("/error/invalid-json")

    exception<Valideringsfeil> { cause ->
        val error = ValideringsError(
            type = invalidParametersType,
            status = HttpStatusCode.UnprocessableEntity.value,
            title = invalidParametersTitle,
            invalidParameters = cause.brudd
        )
        call.respond(HttpStatusCode.UnprocessableEntity, error)
        throw cause
    }

    /**
     * Missing not nullable fields in kotlin data classes
     */
    exception<MissingKotlinParameterException> { cause ->
        val errors: MutableList<Brudd> = mutableListOf()
        cause.path.forEach {
            if (it.fieldName != null) {
                errors.add(
                    Brudd(
                        parameter = it.fieldName,
                        error = "kan ikke være null"
                    ))
            }
        }
        call.respond(
            HttpStatusCode.UnprocessableEntity, ValideringsError(
                status = HttpStatusCode.UnprocessableEntity.value,
                type = invalidParametersType,
                title =  invalidParametersTitle,
                invalidParameters = errors
            )
        )
        throw cause
    }


    /**
     * Properly formatted JSON object, but contains entities on an invalid format
     */
    exception<InvalidFormatException> { cause ->

        val fieldName: String = cause.path.first().fieldName

        call.respond(
            HttpStatusCode.UnprocessableEntity, ValideringsError(
                status = HttpStatusCode.UnprocessableEntity.value,
                type = invalidParametersType,
                title = invalidParametersTitle,
                invalidParameters = listOf(
                    Brudd(
                        parameter = fieldName,
                        error = "${cause.message}"
                    )
                )
            )
        )

        throw cause
    }


    /**
     * Invalid formatted JSON object
     */
    exception<JsonProcessingException> { cause ->

        call.respond(
            HttpStatusCode.BadRequest, DefaultError(
                status = HttpStatusCode.BadRequest.value,
                type = invalidJsonType,
                title = "Requesten er ikke er ikke på gyldig JSON-format"
            )
        )

        throw cause
    }
}