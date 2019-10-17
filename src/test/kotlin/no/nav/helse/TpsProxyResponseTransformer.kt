package no.nav.helse

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import no.nav.helse.tpsproxy.NavHeaders

class TpsProxyResponseTransformer : ResponseTransformer() {
    override fun transform(
        request: Request?,
        response: Response?,
        files: FileSource?,
        parameters: Parameters?
    ): Response {
        val personIdent = request!!.getHeader(NavHeaders.PersonIdent)

        return Response.Builder.like(response)
            .body(getResponse(personIdent))
            .build()
    }

    override fun getName(): String {
        return "tps-proxy-person"
    }

    override fun applyGlobally(): Boolean {
        return false
    }
}

private fun getResponse(navIdent: String) : String {
    val fornavn: String
    val mellomnavn: String
    val etternavn: String
    val foedselsdato: String

    when (navIdent) {
        "01019012345" -> {
            fornavn = "STOR-KAR"
            mellomnavn = "LANGEMANN"
            etternavn = "TEST"
            foedselsdato = "1985-07-27"
        } "25037139184" -> {
            fornavn = "ARNE"
            mellomnavn = "BJARNE"
            etternavn = "CARLSEN"
            foedselsdato = "1990-01-02"
        } else -> {
            fornavn = "CATO"
            mellomnavn = ""
            etternavn = "NILSEN"
            foedselsdato = "1980-05-20"
        }
    }
    return """
    {
        "ident": "$navIdent",
        "identtype": {
            "verdi": "FNR",
            "kodeverk": "Personidenter"
        },
        "kjonn": "M",
        "alder": 48,
        "foedselsdato": "$foedselsdato",
        "foedtILand": {
            "verdi": "NOR",
            "kodeverk": "Landkoder"
        },
        "foedtIKommune": {
            "verdi": "1103",
            "kodeverk": "Kommuner"
        },
        "datoFraOgMed": "2002-05-22",
        "kilde": "SKD",
        "status": {
            "datoFraOgMed": "2002-05-22",
            "kilde": "SKD",
            "kode": {
                "verdi": "BOSA",
                "kodeverk": "Personstatuser"
            }
        },
        "navn": {
            "datoFraOgMed": "2002-05-22",
            "kilde": "SKD",
            "forkortetNavn": "$etternavn $fornavn",
            "slektsnavn": "$etternavn",
            "fornavn": "$fornavn",
            "mellomnavn": "$mellomnavn",
            "slektsnavnUgift": ""
        },
        "spraak": {
            "datoFraOgMed": "2014-09-15",
            "kilde": "PP01",
            "kode": {
                "verdi": "NB",
                "kodeverk": "Språk"
            }
        },
        "sivilstand": {
            "datoFraOgMed": "",
            "kilde": "SKD",
            "kode": {
                "verdi": "UGIF",
                "kodeverk": "Sivilstander"
            }
        },
        "statsborgerskap": {
            "datoFraOgMed": "",
            "kilde": "SKD",
            "kode": {
                "verdi": "NOR",
                "kodeverk": "StatsborgerskapFreg"
            }
        },
        "doedsdato": null,
        "spesiellOpplysning": null,
        "tiltak": null,
        "egenansatt": {
            "datoFraOgMed": null,
            "kilde": null,
            "erEgenansatt": false
        },
        "telefon": {
            "landkodePrivat": null,
            "privat": null,
            "privatDatoRegistrert": null,
            "privatKilde": null,
            "landkodeJobb": null,
            "jobb": null,
            "jobbDatoRegistrert": null,
            "jobbKilde": null,
            "landkodeMobil": "+47",
            "mobil": "48441974",
            "mobilDatoRegistrert": "2016-06-09",
            "mobilKilde": "BD03"
        },
        "adresseinfo": {
            "boadresse": {
                "datoFraOgMed": "2016-09-28",
                "kilde": "SKD",
                "adresse": "HAUSMANNS GATE 40",
                "landkode": "NOR",
                "kommune": "0301",
                "postnummer": "0182",
                "bydel": "030102",
                "adressetillegg": null,
                "veiadresse": {
                    "gatekode": "12782",
                    "husnummer": "40",
                    "bokstav": null,
                    "bolignummer": "H0202"
                },
                "matrikkeladresse": {
                    "gaardsnummer": null,
                    "bruksnummer": null,
                    "festenummer": null,
                    "undernummer": null
                }
            },
            "postadresse": null,
            "prioritertAdresse": {
                "datoFraOgMed": "2014-09-15",
                "kilde": "PP01",
                "kode": {
                    "verdi": "BOAD",
                    "kodeverk": "Adressetyper"
                }
            },
            "geografiskTilknytning": {
                "datoFraOgMed": "2016-09-28",
                "kilde": "SKD",
                "land": null,
                "kommune": null,
                "bydel": "030102"
            },
            "tilleggsadresse": null,
            "utenlandskAdresse": null
        },
        "antallBarn": 1,
        "antallLevendeBarnUnder18": 1,
        "relasjonFinnes": false,
        "foreldreansvar": null,
        "oppholdstillatelse": null,
        "kontonummer": {
            "datoFraOgMed": "2008-09-20",
            "kilde": "IT00",
            "nummer": "96850814136"
        },
        "innvandringUtvandring": null,
        "vergemaalListe": [],
        "brukerbehovListe": [],
        "utenlandsinfoList": [],
        "utenlandskBank": null
    }
    """.trimIndent()
}


