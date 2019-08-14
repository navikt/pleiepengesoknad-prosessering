package no.nav.helse.prosessering.v1

object GraderingUtils {
    private const val INGEN_STOTTE = 0.00
    private const val MAX_STOTTE = 100.00

    private fun faktiskGrad(prosentAvNormalArbeidsUke: Double) = MAX_STOTTE - prosentAvNormalArbeidsUke
    private fun beregnetGrad(faktiskGrad: Double) : Double {
        return when {
            faktiskGrad < 20.00 -> return INGEN_STOTTE
            faktiskGrad >= 90.00 -> return MAX_STOTTE
            else -> faktiskGrad
        }
    }

    internal fun omArbeidsgiversGradering(prosentAvNormalArbeidsUke: Double?) : String? {
        return when (prosentAvNormalArbeidsUke) {
            null -> null
            0.0 -> "Kan ikke jobbe noe for denne arbeidsgiveren. Foreslått grad på ${MAX_STOTTE.formatertMedToDesimaler()}%"
            else -> {
                val faktiskGrad = faktiskGrad(prosentAvNormalArbeidsUke)
                val beregnetGrad = beregnetGrad(faktiskGrad)
                return "Kan jobbe ${prosentAvNormalArbeidsUke.formatertMedToDesimaler()}% for denne arbeidsgiveren. " + when (beregnetGrad) {
                    INGEN_STOTTE -> "Forseslått at inntektstapet ikke er stort nok til å dekkes."
                    MAX_STOTTE -> "Foreslått grad på ${MAX_STOTTE.formatertMedToDesimaler()}%"
                    else -> "Forseslått grad på ${beregnetGrad.formatertMedToDesimaler()}%"
                }
            }
        }
    }
}