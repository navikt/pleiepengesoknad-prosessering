package no.nav.helse.felles

data class Arbeidsforhold(
    val arbeidsform: Arbeidsform,
    val jobberNormaltTimer: Double,
    val erAktivtArbeidsforhold: Boolean? = null,
    val historisk: ArbeidIPeriode? = null,
    val planlagt: ArbeidIPeriode? = null
)

data class ArbeidIPeriode(
    val jobberIPerioden: JobberIPeriodeSvar,
    val jobberSomVanlig: Boolean? = null,
    val enkeltdager: List<Enkeltdag>? = null,
    val fasteDager: PlanUkedager? = null
)

enum class JobberIPeriodeSvar(val pdfTekst: String) {
    JA("Ja"),
    NEI("Nei"),
    VET_IKKE("Vet ikke")
}

enum class Arbeidsform(val utskriftsvennlig: String){
    FAST("Fast antall timer per uke"),
    TURNUS("Turnus"),
    VARIERENDE("Deltid/varierende/tilkalling")
}