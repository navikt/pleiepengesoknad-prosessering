package no.nav.helse.felles

data class Arbeidsforhold(
    val jobberNormaltTimer: Double,
    val historiskArbeid: ArbeidIPeriode? = null,
    val planlagtArbeid: ArbeidIPeriode? = null
)

data class ArbeidIPeriode(
    val jobberIPerioden: JobberIPeriodeSvar,
    val jobberSomVanlig: Boolean? = null,
    val erLiktHverUke: Boolean? = null,
    val enkeltdager: List<Enkeltdag>? = null,
    val fasteDager: PlanUkedager? = null,
    val jobberProsent: Double? = null
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
