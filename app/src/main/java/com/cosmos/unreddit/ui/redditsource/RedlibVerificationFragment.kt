package com.cosmos.unreddit.ui.redditsource

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import com.cosmos.unreddit.databinding.FragmentRedlibVerificationBinding
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.util.LinkValidator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Inline version of [RedlibVerificationActivity]: solves the Cloudflare/Anubis
 * browser challenge of a Redlib instance in a WebView that overlays the feed
 * instead of launching a separate activity.
 *
 * The WebView sends a byte-identical User-Agent and shares the same clearance
 * cookies (via [CookieManager] + [RedditCookieJar]) with the app's OkHttp
 * client, so once the challenge is solved the app's own requests succeed.
 *
 * On "Done" (or when the system back button dismisses it) it posts a
 * [REQUEST_KEY_VERIFICATION] result so the parent can flush the cookies and
 * retry the page the user was on.
 */
@AndroidEntryPoint
class RedlibVerificationFragment : Fragment() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private var _binding: FragmentRedlibVerificationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRedlibVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val requestedUrl = arguments?.getString(EXTRA_URL)
        val instance = arguments?.getString(EXTRA_INSTANCE).orEmpty()

        // Prefer the exact listing the user was on (Cloudflare challenges can be
        // path-specific); fall back to the instance root when unknown.
        val target = requestedUrl
            ?: LinkValidator(instance).validUrl?.toString()
            ?: run {
                dismissOverlay()
                return
            }

        binding.title.text = LinkValidator(target).validUrl?.host ?: instance

        // Let the WebView navigate its own history first; dismiss once it has
        // nowhere left to go. Registered after the feed fragment's callback, so
        // it takes precedence while the overlay is on top.
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.webView.canGoBack()) {
                        binding.webView.goBack()
                    } else {
                        dismissOverlay()
                    }
                }
            }
        )

        val customUserAgent = runBlocking {
            preferencesRepository.getCustomUserAgent().firstOrNull()
        }

        RedlibWebView.applySettings(binding.webView, requireContext(), customUserAgent)
        RedlibWebView.enableCookies(binding.webView)
        binding.webView.webViewClient = RedlibWebView.client(
            context = requireContext(),
            onPageStarted = { binding.progress.isVisible = true },
            onPageFinished = { binding.progress.isVisible = false }
        )

        binding.webView.loadUrl(target)

        binding.done.setOnClickListener {
            // Persist the clearance cookie before leaving so OkHttp can reuse it.
            CookieManager.getInstance().flush()
            setFragmentResult(REQUEST_KEY_VERIFICATION, bundleOf())
            dismissOverlay()
        }
    }

    /**
     * Removes this overlay from the parent fragment's back stack. [Fragment] has
     * no [dismiss] (that is [DialogFragment]-only), so use an explicit
     * transaction against the parent fragment manager.
     */
    private fun dismissOverlay() {
        parentFragmentManager.beginTransaction()
            .remove(this)
            .commitAllowingStateLoss()
    }

    override fun onPause() {
        // Persist challenge cookies even when the user leaves with Back.
        CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.webView.destroy()
        _binding = null
    }

    companion object {
        /** Result key posted when the user completes the inline challenge. */
        const val REQUEST_KEY_VERIFICATION = "redlib_verification"

        private const val EXTRA_URL = "redlib_verification_url"
        private const val EXTRA_INSTANCE = "redlib_verification_instance"

        /**
         * Shows the overlay on top of [parent] (the feed fragment), inside
         * [containerId] (a full-screen host in the feed layout). No-op when a
         * verification overlay is already present.
         */
        fun show(parent: FragmentManager, containerId: Int, url: String?, instance: String) {
            if (parent.findFragmentByTag(REQUEST_KEY_VERIFICATION) != null) return

            val fragment = RedlibVerificationFragment().apply {
                arguments = bundleOf(
                    EXTRA_URL to url,
                    EXTRA_INSTANCE to instance
                )
            }

            parent.beginTransaction()
                .add(containerId, fragment, REQUEST_KEY_VERIFICATION)
                .commit()
        }
    }
}
