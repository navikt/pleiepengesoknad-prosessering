package no.nav.helse

object Authorization {
    fun getAuthorizationHeader() : String {
        return "Bearer foo-bar"
    }
}