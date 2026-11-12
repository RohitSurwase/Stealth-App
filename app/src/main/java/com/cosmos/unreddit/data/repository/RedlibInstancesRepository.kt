package com.cosmos.unreddit.data.repository

import android.content.Context
import com.cosmos.unreddit.data.remote.api.redlib.RedlibInstances
import com.cosmos.unreddit.di.NetworkModule.BasicMoshi
import com.cosmos.unreddit.di.NetworkModule.GenericOkHttp
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the list of public Redlib instances.
 *
 * The list is downloaded from the redlib-org/redlib-instances repository and
 * cached in SharedPreferences so that every dialog opening does not hit GitHub.
 * The network is only used when there is no cached list or when the user
 * explicitly refreshes it.
 */
@Singleton
class RedlibInstancesRepository @Inject constructor(
    @GenericOkHttp private val okHttpClient: OkHttpClient,
    @BasicMoshi moshi: Moshi,
    @ApplicationContext context: Context
) {

    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val adapter = moshi.adapter(RedlibInstances::class.java)

    fun getCachedInstances(): List<String> {
        return preferences.getString(KEY_INSTANCES, null)
            ?.split('\n')
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    /**
     * Returns the cached instances, downloading them only when the cache is
     * empty or [forceRefresh] is set.
     */
    suspend fun getInstances(forceRefresh: Boolean = false): Result<List<String>> {
        if (!forceRefresh) {
            getCachedInstances()
                .takeIf { it.isNotEmpty() }
                ?.let { return Result.success(it) }
        }

        return withContext(Dispatchers.IO) {
            runCatching { downloadInstances() }
        }
    }

    private fun downloadInstances(): List<String> {
        val request = Request.Builder()
            .url(INSTANCES_URL)
            .build()

        val body = okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            response.body?.string() ?: throw IOException("Empty response")
        }

        val parsed = adapter.fromJson(body) ?: throw IOException("Invalid JSON")

        val hosts = parsed.instances
            .mapNotNull { it.url?.toHttpUrlOrNull()?.host }
            .filter { host ->
                host.isNotBlank() &&
                    !host.endsWith(".onion", ignoreCase = true) &&
                    !host.endsWith(".i2p", ignoreCase = true)
            }
            .distinct()

        if (hosts.isEmpty()) {
            throw IOException("No clearnet instances in the list")
        }

        preferences.edit()
            .putString(KEY_INSTANCES, hosts.joinToString("\n"))
            .apply()

        return hosts
    }

    private companion object {
        const val INSTANCES_URL =
            "https://raw.githubusercontent.com/redlib-org/redlib-instances/main/instances.json"
        const val PREFERENCES_NAME = "redlib_instances"
        const val KEY_INSTANCES = "instances"
    }
}
