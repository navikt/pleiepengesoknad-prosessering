package no.nav.helse.systembruker

import java.time.LocalDateTime

class SystembrukerService(
    private val systembrukerGateway: SystembrukerGateway
) {

    @Volatile private var cachedToken: String? = null
    @Volatile private var expiry: LocalDateTime? = null

    private suspend fun getToken() : String {
        if (hasCachedToken() && isCachedTokenValid()) {
            return cachedToken!!
        }

        clearCachedData()

        val response = systembrukerGateway.getToken()
        setCachedData(response)
        return cachedToken!!
    }

    suspend fun getAuthorizationHeader() : String {
        return "Bearer ${getToken()}"
    }

    private fun setCachedData(response: Response) {
        cachedToken = response.accessToken
        expiry = LocalDateTime.now()
            .plusSeconds(response.expiresIn)
            .minusSeconds(10L)
    }

    private fun clearCachedData() {
        cachedToken = null
        expiry = null
    }

    private fun hasCachedToken() : Boolean {
        return cachedToken != null && expiry != null
    }

    private fun isCachedTokenValid() : Boolean {
        return expiry!!.isAfter(LocalDateTime.now())
    }
}