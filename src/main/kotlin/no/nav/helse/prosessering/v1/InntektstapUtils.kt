package no.nav.helse.prosessering.v1

object InntektstapUtils {
    private const val MAX_INNTEKTSTAP = 100.00

    internal fun innktektstap(prosentAvNormalArbeidsUke: Double?) = if (prosentAvNormalArbeidsUke == null) null else MAX_INNTEKTSTAP - prosentAvNormalArbeidsUke

//    internal fun omArbeidsgiversGradering(prosentAvNormalArbeidsUke: Double?) : String? {
//        return when (prosentAvNormalArbeidsUke) {
//            null -> null
//            0.0 -> "Har et inntektstap på ${MAX_INNTEKTSTAP.formatertMedToDesimaler()}%"
//            else -> "Har et inntektstap på ${innktektstap(prosentAvNormalArbeidsUke).formatertMedToDesimaler()}%"
//        }
//    }
}