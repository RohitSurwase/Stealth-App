package com.cosmos.unreddit.ui.redditsource

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.cosmos.unreddit.databinding.ActivityRedlibVerificationBinding
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

        RedlibWebView.applySettings(binding.webView, this, customUserAgent)
        RedlibWebView.enableCookies(binding.webView)
        binding.webView.webViewClient = RedlibWebView.client(
            context = this,
            onPageStarted = { binding.progress.isVisible = true },
            onPageFinished = { binding.progress.isVisible = false }
        )

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
        private const val EXTRA_INSTANCE = "EXTRA_INSTANCE"

        fun intent(context: Context, instance: String): Intent {
            return Intent(context, RedlibVerificationActivity::class.java)
                .putExtra(EXTRA_INSTANCE, instance)
        }
    }
}
