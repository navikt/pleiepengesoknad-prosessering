package no.nav.helse.prosessering.v1

object InntektstapUtils {
    private const val MAX_INNTEKTSTAP = 100.00

    internal fun innktektstap(prosentAvNormalArbeidsUke: Double?) = if (prosentAvNormalArbeidsUke == null) null else MAX_INNTEKTSTAP - prosentAvNormalArbeidsUke
}