package com.cosmos.unreddit.util

import com.cosmos.unreddit.data.model.db.Redirect
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.di.DispatchersModule.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkRedirector @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    private val mutex = Mutex()

    var isPrivacyEnhancerOn: Boolean = runBlocking {
        preferencesRepository.getPrivacyEnhancerEnabled().first()
    }
        private set

    private var toRedirect: Map<Regex, Redirect> = runBlocking {
        preferencesRepository.getAllRedirects().first().getRedirects()
    }

    fun getRedirectLink(link: String): LinkRedirect? {
        val url = link.toHttpUrlOrNull() ?: return null
        val redirect = toRedirect.firstNotNullOfOrNull { entry ->
            entry.value.takeIf { it.mode.isEnabled && url.host.matches(entry.key) }
        }
        val redirectUrl = redirect?.getRedirectLink(url)
        return redirectUrl?.let { LinkRedirect(url, it, redirect.mode) }
    }

    suspend fun setPrivacyEnhancerEnabled(enable: Boolean) {
        mutex.withLock {
            isPrivacyEnhancerOn = enable
        }
    }

    suspend fun setRedirectList(redirects: List<Redirect>) {
        mutex.withLock {
            toRedirect = redirects.getRedirects()
        }
    }

    private fun Redirect.getRedirectLink(url: HttpUrl): HttpUrl {
        return url.newBuilder().host(redirect).build()
    }

    private suspend fun List<Redirect>.getRedirects(): Map<Regex, Redirect> {
        return withContext(defaultDispatcher) { associateBy { Regex(it.pattern) } }
    }

    data class LinkRedirect(
        val original: HttpUrl,
        val redirect: HttpUrl,
        val mode: Redirect.RedirectMode
    )
}
