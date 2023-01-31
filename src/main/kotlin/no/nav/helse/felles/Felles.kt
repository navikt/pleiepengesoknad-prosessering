package no.nav.helse.felles

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

enum class BarnRelasjon(val utskriftsvennlig: String) {
    MOR("Du er mor til barnet"),
    MEDMOR("Du er medmor til barnet"),
    FAR("Du er far til barnet"),
    FOSTERFORELDER("Du er fosterforelder til barnet"),
    ANNET("Annet")
}

data class Søker(
    val aktørId: String,
    val fødselsnummer: String,
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String
) {
    override fun toString(): String {
        return "Soker()"
    }
}

data class Barn(
    val navn : String,
    val fødselsnummer: String? = null,
    val fødselsdato: LocalDate? = null,
    val aktørId: String? = null, // Brukes av sif-innsyn-api
    val årsakManglerIdentitetsnummer: ÅrsakManglerIdentitetsnummer? = null
    ) {
    override fun toString(): String {
        return "Barn()"
    }
}

enum class ÅrsakManglerIdentitetsnummer(val pdfTekst: String) {
    NYFØDT ("Barnet er nyfødt, og har ikke fått fødselsnummer enda"),
    BARNET_BOR_I_UTLANDET ("Barnet bor i utlandet"),
    ANNET ("Annet")
}

data class Medlemskap(
    val harBoddIUtlandetSiste12Mnd : Boolean,
    val utenlandsoppholdSiste12Mnd: List<Bosted> = listOf(),
    val skalBoIUtlandetNeste12Mnd : Boolean,
    val utenlandsoppholdNeste12Mnd: List<Bosted> = listOf()
)

data class Nattevåk(
    val harNattevåk: Boolean,
    val tilleggsinformasjon: String?
) {
    override fun toString(): String {
        return "Nattevåk(harNattevåk=$harNattevåk)"
    }
}

data class Frilans(
    val harInntektSomFrilanser: Boolean,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val startdato: LocalDate? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val sluttdato: LocalDate? = null,
    val jobberFortsattSomFrilans: Boolean? = null,
    val arbeidsforhold: Arbeidsforhold? = null,
    val frilansTyper: List<FrilansType>? = null,
    val misterHonorarer: Boolean? = null,
    val misterHonorarerIPerioden: MisterHonorarerFraVervIPerioden? = null
)

enum class MisterHonorarerFraVervIPerioden {
    MISTER_ALLE_HONORARER, MISTER_DELER_AV_HONORARER
}

enum class FrilansType {
    FRILANS,
    STYREVERV
}

data class Beredskap(
    val beredskap: Boolean,
    val tilleggsinformasjon: String?
) {
    override fun toString(): String {
        return "Beredskap(beredskap=$beredskap)"
    }
}

data class Bosted(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val tilOgMed: LocalDate,
    val landkode: String,
    val landnavn: String
) {
    override fun toString(): String {
        return "Utenlandsopphold(fraOgMed=$fraOgMed, tilOgMed=$tilOgMed, landkode='$landkode', landnavn='$landnavn')"
    }
}

data class Utenlandsopphold(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val tilOgMed: LocalDate,
    val landkode: String,
    val landnavn: String,
    val erUtenforEøs: Boolean?,
    val erBarnetInnlagt: Boolean?,
    val perioderBarnetErInnlagt: List<Periode> = listOf(),
    val årsak: Årsak?
) {
    override fun toString(): String {
        return "Utenlandsopphold(fraOgMed=$fraOgMed, tilOgMed=$tilOgMed, landkode='$landkode', landnavn='$landnavn', erUtenforEøs=$erUtenforEøs, erBarnetInnlagt=$erBarnetInnlagt, årsak=$årsak)"
    }
}

data class Periode(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate
)

enum class Årsak(val beskrivelse: String) {
    BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING("Barnet innlagt i helseinstitusjon for norsk offentlig regning"),
    BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD("Barnet innlagt i helseinstitusjon dekket etter avtale med et annet land om trygd"),
    ANNET("Innleggelsen dekkes av søker selv"),
}

data class UtenlandsoppholdIPerioden(
    val skalOppholdeSegIUtlandetIPerioden: Boolean,
    val opphold: List<Utenlandsopphold> = listOf()
)

data class FerieuttakIPerioden(
    val skalTaUtFerieIPerioden: Boolean,
    val ferieuttak: List<Ferieuttak>
) {
    override fun toString(): String {
        return "FerieuttakIPerioden(skalTaUtFerieIPerioden=$skalTaUtFerieIPerioden, ferieuttak=$ferieuttak)"
    }
}

data class Ferieuttak(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val tilOgMed: LocalDate
) {
    override fun toString(): String {
        return "Ferieuttak(fraOgMed=$fraOgMed, tilOgMed=$tilOgMed)"
    }
}

fun Søker.tilTpsNavn(): Navn = Navn(
    fornavn = fornavn,
    mellomnavn = mellomnavn,
    etternavn = etternavn
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
)
