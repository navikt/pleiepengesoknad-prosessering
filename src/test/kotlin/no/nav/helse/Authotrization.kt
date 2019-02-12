package no.nav.helse

import no.nav.security.oidc.test.support.JwtTokenGenerator

object Authorization {
    fun getAccessToken(
        issuer: String,
        subject: String
    ) : String {
        val claimSet = JwtTokenGenerator.buildClaimSet(
            subject,
            issuer,
            "localhost",
            JwtTokenGenerator.EXPIRY
        )

        return JwtTokenGenerator.createSignedJWT(claimSet).serialize()
    }
}