package com.cosmos.unreddit.ui.redditsource

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.cosmos.unreddit.databinding.ActivityRedlibVerificationBinding
import com.cosmos.unreddit.data.remote.BrowserUserAgent
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.util.LinkValidator
import com.cosmos.unreddit.util.extension.unredditApplication
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Lets the user complete the Cloudflare/Anubis browser challenge of a Redlib
 * instance once. The cookies obtained here are stored in [CookieManager] and
 * reused by `RedditCookieJar` for the app's own OkHttp requests.
 */
@AndroidEntryPoint
class RedlibVerificationActivity : AppCompatActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private lateinit var binding: ActivityRedlibVerificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(unredditApplication.appTheme)
        super.onCreate(savedInstanceState)

        binding = ActivityRedlibVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val instance = intent.getStringExtra(EXTRA_INSTANCE)
        val url = instance?.let { LinkValidator(it).validUrl }

        if (url == null) {
            finish()
            return
        }

        binding.title.text = url.host

        val customUserAgent = runBlocking {
            preferencesRepository.getCustomUserAgent().firstOrNull()
        }

        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            // The verification page never needs local files/content providers.
            allowFileAccess = false
            allowContentAccess = false
            // Must match the User-Agent used by OkHttp, otherwise Cloudflare's
            // clearance cookie will be rejected on the app's requests.
            userAgentString = BrowserUserAgent.resolve(
                this@RedlibVerificationActivity,
                customUserAgent
            )
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(binding.webView, true)
        }

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.progress.isVisible = true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progress.isVisible = false
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                Log.w(TAG, "error ${request?.url}: ${error?.description}")
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                Log.w(TAG, "http ${errorResponse?.statusCode} ${request?.url}")
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val path = request?.url?.path ?: return null
                // Cloudflare answers 403 for Anubis' worker script when it is
                // requested from an Android WebView. Serve the equivalent
                // upstream worker locally so the proof-of-work can complete.
                if (path.contains("/.within.website/x/cmd/anubis/static/js/worker/sha256-") &&
                    path.endsWith(".mjs")
                ) {
                    return WebResourceResponse(
                        "text/javascript",
                        "utf-8",
                        200,
                        "OK",
                        mapOf("Cache-Control" to "no-store"),
                        assets.open("anubis/sha256-webcrypto.mjs")
                    )
                }
                return null
            }
        }

        binding.webView.loadUrl(url.toString())

        binding.done.setOnClickListener {
            CookieManager.getInstance().flush()
            finish()
        }
    }

    override fun onPause() {
        // Persist challenge cookies even when the user leaves with Back.
        CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "RedlibWebView"
        private const val EXTRA_INSTANCE = "EXTRA_INSTANCE"

        fun intent(context: Context, instance: String): Intent {
            return Intent(context, RedlibVerificationActivity::class.java)
                .putExtra(EXTRA_INSTANCE, instance)
        }
    }
}
