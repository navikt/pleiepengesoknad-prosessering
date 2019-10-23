package no.nav.helse

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response

private val identMap = mapOf(
    "AktoerId" to "12345",
    "NorskIdent" to "678910"
)

class AktoerRegisterResponseTransformer : ResponseTransformer() {
    override fun transform(
        request: Request?,
        response: Response?,
        files: FileSource?,
        parameters: Parameters?
    ): Response {
        val personIdent = request!!.getHeader("Nav-Personidenter")
        val identGruppe = request.queryParameter("identgruppe").firstValue()


        return Response.Builder.like(response)
            .body(getResponse(
                personIdent = personIdent,
                identGruppe = identGruppe
            ))
            .build()
    }

    override fun getName(): String {
        return "aktoer-register"
    }

    override fun applyGlobally(): Boolean {
        return false
    }

}

private fun getResponse(
    personIdent: String,
    identGruppe: String
) = """
{
  "$personIdent": {
    "identer": [
      {
        "ident": "${identMap[identGruppe]}",
        "identgruppe": "$identGruppe",
        "gjeldende": true
      }
    ],
    "feilmelding": null
  }
}
""".trimIndent()