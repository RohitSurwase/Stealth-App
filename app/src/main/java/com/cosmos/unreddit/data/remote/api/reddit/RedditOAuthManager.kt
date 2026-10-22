package com.cosmos.unreddit.data.remote.api.reddit

import android.content.Context
import android.util.Base64
import android.util.Log
import com.cosmos.unreddit.di.NetworkModule
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.Random
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class RedditOAuthCredentials(
    val authorization: String,
    val userAgent: String,
    val deviceId: String,
    val loid: String?,
    val session: String?,
    val qos: String,
    val codecs: String,
    val expiresAt: Long
)

@Singleton
class RedditOAuthManager @Inject constructor(
    @ApplicationContext context: Context,
    @NetworkModule.GenericOkHttp private val client: OkHttpClient
) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val lock = Any()

    @Volatile
    private var credentials: RedditOAuthCredentials? = null

    fun getCredentials(): RedditOAuthCredentials? {
        val current = credentials
        if (current != null && current.expiresAt > System.currentTimeMillis()) {
            return current
        }

        synchronized(lock) {
            val cached = credentials
            if (cached != null && cached.expiresAt > System.currentTimeMillis()) {
                return cached
            }

            val refreshed = refresh()
            credentials = refreshed
            return refreshed
        }
    }

    fun invalidate() {
        synchronized(lock) {
            credentials = null
        }
    }

    private fun refresh(): RedditOAuthCredentials? {
        val deviceId = preferences.getString(KEY_DEVICE_ID, null)
            ?: UUID.randomUUID().toString().also {
                preferences.edit().putString(KEY_DEVICE_ID, it).apply()
            }
        val qos = String.format(Locale.US, "%.3f", 1.0 + Random().nextDouble() * 99.0)

        val request = Request.Builder()
            .url(TOKEN_URL)
            .header("User-Agent", USER_AGENT)
            .header("x-reddit-retry", "algo=no-retries")
            .header("x-reddit-compression", "1")
            .header("x-reddit-qos", qos)
            .header("x-reddit-media-codecs", CODECS)
            .header("Content-Type", "application/json; charset=UTF-8")
            .header("client-vendor-id", deviceId)
            .header("X-Reddit-Device-Id", deviceId)
            .header("Authorization", "Basic ${basicAuthorization()}")
            .post(TOKEN_BODY.toRequestBody(JSON))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body.isNullOrBlank()) {
                    Log.e(TAG, "Token request failed with ${response.code}")
                    return null
                }

                val json = JSONObject(body)
                val token = json.optString("access_token")
                if (token.isBlank()) {
                    Log.e(TAG, "Token response did not contain an access token")
                    return null
                }

                val expiresIn = json.optLong("expires_in", DEFAULT_EXPIRES_IN)
                RedditOAuthCredentials(
                    authorization = "Bearer $token",
                    userAgent = USER_AGENT,
                    deviceId = deviceId,
                    loid = response.header("x-reddit-loid"),
                    session = response.header("x-reddit-session"),
                    qos = qos,
                    codecs = CODECS,
                    expiresAt = System.currentTimeMillis() +
                        (expiresIn - TOKEN_EXPIRY_MARGIN).coerceAtLeast(60L) * 1000L
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token request failed", e)
            null
        }
    }

    private fun basicAuthorization(): String {
        return Base64.encodeToString("$CLIENT_ID:".toByteArray(), Base64.NO_WRAP)
    }

    companion object {
        private const val TAG = "RedditOAuth"
        private const val PREFERENCES_NAME = "reddit_oauth"
        private const val KEY_DEVICE_ID = "device_id"
        private const val CLIENT_ID = "ohXpoqrZYub1kg"
        private const val TOKEN_URL = "https://www.reddit.com/auth/v2/oauth/access-token/loid"
        private const val USER_AGENT = "Reddit/Version 2024.47.0/Build 2029755/Android 14"
        private const val CODECS = "available-codecs=video/avc, video/hevc"
        private const val TOKEN_BODY = "{\"scopes\":[\"*\",\"email\",\"pii\"]}"
        private const val DEFAULT_EXPIRES_IN = 3600L
        private const val TOKEN_EXPIRY_MARGIN = 120L
        private val JSON = "application/json; charset=UTF-8".toMediaType()
    }
}
