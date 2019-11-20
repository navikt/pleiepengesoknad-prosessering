package no.nav.helse

import no.nav.helse.aktoer.Fodselsnummer
import no.nav.helse.prosessering.v1.*
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.Ignore
import kotlin.test.Test

class PdfV1GeneratorTest {

    private companion object {
        private val generator = PdfV1Generator()
        private val barnetsIdent = Fodselsnummer("02119970078")
        private val barnetsNavn = "Ole Dole"
    }

    private fun gyldigMelding(
        soknadId: String,
        sprak: String? = "nb",
        organisasjoner: List<Organisasjon> = listOf(
            Organisasjon(
                organisasjonsnummer = "987564785",
                navn = "NAV"
            ),
            Organisasjon(
                organisasjonsnummer = "975124568",
                navn = "Kiwi"
            ),
            Organisasjon(
                organisasjonsnummer = "952352687",
                navn = "Bjerkheim gård"
            ),
            Organisasjon(
                organisasjonsnummer = "952352655",
                navn = "Hopp i havet"
            )
        ),
        barn: Barn = Barn(
            navn = "Børge Øverbø Ånsnes",
            fodselsnummer = null,
            alternativId = "29091884321",
            aktoerId = null
        ),
        grad: Int? = 60,
        harMedsoker: Boolean = true,
        samtidigHjemme: Boolean? = false,
        dagerPerUkeBorteFraJobb: Double? = 4.5,
        tilsynsordning: Tilsynsordning? = null,
        beredskap: Beredskap? = null,
        nattevaak: Nattevaak? = null
    ) = MeldingV1(
        sprak = sprak,
        soknadId = soknadId,
        mottatt = ZonedDateTime.now(),
        fraOgMed = LocalDate.now().plusDays(6),
        tilOgMed = LocalDate.now().plusDays(35),
        soker = Soker(
            aktoerId = "123456",
            fornavn = "Ærling",
            mellomnavn = "Øverbø",
            etternavn = "Ånsnes",
            fodselsnummer = "29099012345"
        ),
        barn = barn,
        relasjonTilBarnet = "Onkel & Nærstående ' <> \" {}",
        arbeidsgivere = Arbeidsgivere(
            organisasjoner = organisasjoner
        ),
        medlemskap = Medlemskap(
            harBoddIUtlandetSiste12Mnd = true,
            skalBoIUtlandetNeste12Mnd = false
        ),
        grad = grad,
        harMedsoker = harMedsoker,
        samtidigHjemme = samtidigHjemme,
        harForstattRettigheterOgPlikter = true,
        harBekreftetOpplysninger = true,
        dagerPerUkeBorteFraJobb = dagerPerUkeBorteFraJobb,
        tilsynsordning = tilsynsordning,
        nattevaak = nattevaak,
        beredskap = beredskap
    )

    private fun genererOppsummeringsPdfer(writeBytes: Boolean) {
        var id = "1-full"
        var pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(soknadId = id),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "2-utenMedsoker"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(soknadId = id, harMedsoker = false),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "3-utenSprak"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(soknadId = id, harMedsoker = false, sprak = null),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "4-utenArbeidsgivere"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(soknadId = id, harMedsoker = false, organisasjoner = listOf()),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "5-utenInfoPaaBarn"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsoker = false,
                organisasjoner = listOf(),
                barn = Barn(fodselsnummer = null, alternativId = null, navn = null, aktoerId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "6-utenGrad"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsoker = false,
                organisasjoner = listOf(),
                barn = Barn(fodselsnummer = null, alternativId = null, navn = null, aktoerId = null),
                grad = null
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "7-utenDagerBorteFraJobb"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsoker = false,
                organisasjoner = listOf(),
                barn = Barn(fodselsnummer = null, alternativId = null, navn = null, aktoerId = null),
                grad = null,
                dagerPerUkeBorteFraJobb = null
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "8-medTilsynsOrdningJa"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsoker = false,
                barn = Barn(fodselsnummer = null, alternativId = null, navn = null, aktoerId = null),
                grad = null,
                dagerPerUkeBorteFraJobb = null,
                tilsynsordning = Tilsynsordning(
                    svar = "ja",
                    ja = TilsynsordningJa(
                        mandag = Duration.ofHours(0).plusMinutes(0),
                        tirsdag = Duration.ofHours(7).plusMinutes(55),
                        onsdag = null,
                        torsdag = Duration.ofHours(1).plusMinutes(1),
                        fredag = Duration.ofMinutes(43),
                        tilleggsinformasjon = "Unntatt uke 43, da skal han være hos pappaen sin.\rmed\nlinje\r\nlinjeskift."
                    ),
                    vetIkke = null
                ),
                beredskap = Beredskap(
                    beredskap = true,
                    tilleggsinformasjon = "Jeg er i beredskap\rmed\nlinje\r\nlinjeskift."
                ),
                nattevaak = Nattevaak(
                    harNattevaak = false,
                    tilleggsinformasjon = null
                ),
                organisasjoner = listOf(
                    Organisasjon(
                        organisasjonsnummer = "987564785",
                        navn = "NAV",
                        skalJobbeProsent = 22.5
                    ),
                    Organisasjon(
                        organisasjonsnummer = "975124568",
                        navn = "Kiwi",
                        skalJobbeProsent = 88.3123
                    )
                )
            ), barnetsIdent = barnetsIdent, barnetsNavn = barnetsNavn
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "9-medTilsynsordningVetIkke"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsoker = false,
                organisasjoner = listOf(),
                barn = Barn(fodselsnummer = null, alternativId = null, navn = null, aktoerId = null),
                grad = null,
                dagerPerUkeBorteFraJobb = null,
                tilsynsordning = Tilsynsordning(
                    svar = "vet_ikke",
                    ja = null,
                    vetIkke = TilsynsordningVetIkke(
                        svar = "annet",
                        annet = "Jeg har ingen anelse om dette\rmed\nlinje\r\nlinjeskift."
                    )
                )
            ), barnetsIdent = barnetsIdent, barnetsNavn = barnetsNavn
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)


        id = "10-medTilsynsordningNei"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsoker = false,
                organisasjoner = listOf(),
                barn = Barn(fodselsnummer = null, alternativId = null, navn = null, aktoerId = null),
                grad = null,
                dagerPerUkeBorteFraJobb = null,
                tilsynsordning = Tilsynsordning(
                    svar = "nei",
                    ja = null,
                    vetIkke = null
                )
            ), barnetsIdent = barnetsIdent, barnetsNavn = barnetsNavn
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "11-SkalJobbeProsenterAvNormaltid"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsoker = true, organisasjoner = listOf(
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Hopp i havet",
                        jobberNormaltTimer = 30.0,
                        skalJobbeProsent = 50.0
                    )
                ), barn = Barn(fodselsnummer = null, alternativId = null, navn = null, aktoerId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "12-vetIkkeHvorMyeVedkommendeSkalJobbe"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id, harMedsoker = true, samtidigHjemme = true, organisasjoner = listOf(
                    Organisasjon(
                        organisasjonsnummer = "952352655",
                        navn = "Hopp i havet",
                        jobberNormaltTimer = 30.0,
                        vetIkkeEkstrainfo = "Liker ikke å jobbe..."
                    )
                ), barn = Barn(fodselsnummer = null, alternativId = null, navn = null, aktoerId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "13-samtidigHjemme"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = gyldigMelding(
                soknadId = id,
                harMedsoker = true,
                samtidigHjemme = true,
                barn = Barn(fodselsnummer = null, alternativId = null, navn = null, aktoerId = null)
            ),
            barnetsIdent = barnetsIdent,
            barnetsNavn = barnetsNavn
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)
    }

    private fun pdfPath(soknadId: String) = "${System.getProperty("user.dir")}/generated-pdf-$soknadId.pdf"

    @Test
    fun `generering av oppsummerings-PDF fungerer`() {
        genererOppsummeringsPdfer(false)
    }

    @Test
   // @Ignore
    fun `opprett lesbar oppsummerings-PDF`() {
        genererOppsummeringsPdfer(true)
    }
}