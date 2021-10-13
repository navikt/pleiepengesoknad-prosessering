package no.nav.helse.felles

data class Arbeidsforhold(
    val arbeidsform: Arbeidsform,
    val jobberNormaltTimer: Double,
    val historiskArbeid: ArbeidIPeriode? = null,
    val planlagtArbeid: ArbeidIPeriode? = null
)

data class ArbeidIPeriode(
    val jobberIPerioden: JobberIPeriodeSvar,
    val jobberSomVanlig: Boolean? = null,
    val erLiktHverUke: Boolean? = null,
    val enkeltdager: List<Enkeltdag>? = null,
    val fasteDager: PlanUkedager? = null
)

enum class JobberIPeriodeSvar(val pdfTekst: String) {
    JA("Ja"),
    NEI("Nei"),
    VET_IKKE("Vet ikke");

    fun tilBoolean(): Boolean{
        return when(this){
            JA -> true
            NEI, VET_IKKE -> false
        }
    }
}

enum class Arbeidsform(val utskriftsvennlig: String){
    FAST("Jobber fast antall timer per uke"),
    TURNUS("Jobber turnus"),
    VARIERENDE("Jobber deltid/varierende/tilkalling")
}
