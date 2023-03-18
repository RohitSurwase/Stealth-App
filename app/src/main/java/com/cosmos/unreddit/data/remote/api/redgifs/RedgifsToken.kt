package com.cosmos.unreddit.data.remote.api.redgifs

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RedgifsToken @Inject constructor(private val redgifsApi: RedgifsApi) {

    private var token: String? = null

    private suspend fun getToken(): String? {
        if (token != null) return token!!

        runCatching {
            redgifsApi.getTemporaryToken()
        }.onSuccess {
            token = it.token
        }

        return token
    }

    suspend fun getAuthorization(): String {
        val token = getToken() ?: ""
        return "Bearer $token"
    }
}
