package com.cosmos.unreddit.ui.linkmenu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.MediaType
import com.cosmos.unreddit.databinding.FragmentLinkMenuBinding
import com.cosmos.unreddit.util.extension.doAndDismiss
import com.cosmos.unreddit.util.extension.openExternalLink
import com.cosmos.unreddit.util.extension.shareExternalLink
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class LinkMenuFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentLinkMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLinkMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val link = arguments?.getString(BUNDLE_KEY_LINK)
        link?.let {
            binding.link = it
            val type = when {
                it.startsWith("/u/") -> MediaType.REDDIT_USER
                it.startsWith("/r/") -> MediaType.REDDIT_SUBREDDIT
                else -> MediaType.LINK
            }
            initActions(it, type)
        }
    }

    private fun initActions(link: String, type: MediaType) {
        with(binding) {
            buttonOpen.setOnClickListener {
                val url = when (type) {
                    MediaType.REDDIT_USER,
                    MediaType.REDDIT_SUBREDDIT -> "https://www.reddit.com$link"
                    else -> link
                }
                doAndDismiss { openExternalLink(url) }
            }

            buttonShareLink.setOnClickListener {
                doAndDismiss { shareExternalLink(link) }
            }

            buttonCopyLink.setOnClickListener {
                val clipboard =
                    activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?

                val clip = ClipData.newPlainText("link from Reddit", link)

                doAndDismiss {
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip)
                        // TODO: Warn user link was copied
                    } else {
                        // TODO: Warn user link was NOT copied
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getTheme(): Int {
        return R.style.PostDetailsSheetTheme
    }

    companion object {
        private const val TAG = "LinkMenuFragment"

        private const val BUNDLE_KEY_LINK = "BUNDLE_KEY_LINK"

        fun show(fragmentManager: FragmentManager, link: String) {
            LinkMenuFragment().apply {
                arguments = bundleOf(
                    BUNDLE_KEY_LINK to link,
                )
            }.show(fragmentManager, TAG)
        }
    }
}
