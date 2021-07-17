package com.cosmos.unreddit.ui.browser

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.cosmos.unreddit.data.model.MediaType
import com.cosmos.unreddit.databinding.FragmentBrowserBinding
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.util.LinkUtil
import com.cosmos.unreddit.util.extension.openExternalLink

class BrowserFragment : BaseFragment() {

    private var _binding: FragmentBrowserBinding? = null
    private val binding get() = _binding!!

    private val args: BrowserFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAppBar()
        initWebView()
        binding.loadingCradle.isVisible = true
        if (savedInstanceState != null) {
            binding.webview.restoreState(savedInstanceState)
        } else {
            binding.webview.loadUrl(args.url)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webview.saveState(outState)
    }

    private fun initAppBar() {
        binding.appBar.run {
            backCard.setOnClickListener { findNavController().navigateUp() }
            refreshCard.setOnClickListener { binding.webview.reload() }
            openCard.setOnClickListener {
                binding.webview.url?.let { openExternalLink(it) }
            }
        }
    }

    @Suppress("SetJavaScriptEnabled")
    private fun initWebView() {
        binding.webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                request?.url?.toString()?.let { url ->
                    val mediaType = LinkUtil.getLinkType(url)
                    if (mediaType != MediaType.LINK) {
                        onLinkClick(url, mediaType)
                        return true
                    }
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.loadingCradle.isVisible = true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.appBar.label.text = view?.title
                binding.loadingCradle.isVisible = false
            }
        }
        binding.webview.settings.javaScriptEnabled = true
    }

    override fun onBackPressed() {
        binding.webview.run {
            if (canGoBack()) {
                goBack()
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.loadingCradle.stop()
        binding.webview.run {
            removeAllViews()
            destroy()
        }
        _binding = null
    }
}
