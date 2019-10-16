package no.nav.helse.tpsproxy

internal class TpsProxyV1Gateway(
    private val tpsProxyV1: TpsProxyV1
) {
    internal companion object {
        private val barnAttributter = setOf(
            Attributt.barnAktørId, // Må hente opp barn for å vite hvem vi skal slå opp aktørId på
            Attributt.barnFornavn,
            Attributt.barnMellomnavn,
            Attributt.barnEtternavn,
            Attributt.barnFødselsdato
        )
    }

    internal suspend fun barn(
        ident: Ident
    ): Set<TpsBarn>? {
       return tpsProxyV1.barn(ident)
    }
}

internal enum class Attributt(internal val api: String) {
    aktørId("aktør_id"),
    fornavn("fornavn"),
    mellomnavn("mellomnavn"),
    etternavn("etternavn"),
    fødselsdato("fødselsdato"),

    barnAktørId("barn[].aktør_id"),
    barnFornavn("barn[].fornavn"),
    barnMellomnavn("barn[].mellomnavn"),
    barnEtternavn("barn[].etternavn"),
    barnFødselsdato("barn[].fødselsdato"),

    arbeidsgivereOrganisasjonerNavn("arbeidsgivere[].organisasjoner[].navn"),
    arbeidsgivereOrganisasjonerOrganisasjonsnummer("arbeidsgivere[].organisasjoner[].organisasjonsnummer")

    ;

    internal companion object {
        internal fun fraApi(api: String): Attributt {
            for (value in values()) {
                if (value.api == api) return value
            }
            throw IllegalStateException("$api er ikke en støttet attributt.")
        }
    }
}