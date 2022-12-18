package com.cosmos.unreddit.util

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.GalleryMedia
import com.cosmos.unreddit.data.model.MediaType
import com.cosmos.unreddit.data.model.db.Redirect
import com.cosmos.unreddit.data.model.db.Redirect.RedirectMode.ALWAYS_ASK
import com.cosmos.unreddit.data.model.db.Redirect.RedirectMode.OFF
import com.cosmos.unreddit.data.model.db.Redirect.RedirectMode.ON
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.ui.mediaviewer.MediaViewerFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.scopes.FragmentScoped
import okhttp3.HttpUrl
import javax.inject.Inject

@FragmentScoped
class LinkHandler @Inject constructor(
    private val fragment: Fragment,
    private val linkRedirector: LinkRedirector
) {

    private val baseFragment: BaseFragment?
        get() = fragment as? BaseFragment

    fun handleLink(link: String) {
        val mediaType = LinkUtil.getLinkType(link)
        handleLink(link, mediaType)
    }

    private fun handleLink(link: String, mediaType: MediaType) {
        when (mediaType) {
            MediaType.REDDIT_SUBREDDIT -> {
                val subreddit = link.removePrefix("/r/")
                baseFragment?.openSubreddit(subreddit)
            }

            MediaType.REDDIT_USER -> {
                val user = link.removePrefix("/u/")
                baseFragment?.openUser(user)
            }

            MediaType.REDDIT_LINK -> baseFragment?.openRedditLink(link)

            MediaType.REDDIT_PERMALINK -> {
                val post = "https://www.reddit.com$link"
                baseFragment?.openRedditLink(post)
            }

            MediaType.IMGUR_ALBUM,
            MediaType.IMGUR_GALLERY,
            MediaType.IMGUR_GIF,
            MediaType.IMGUR_VIDEO,
            MediaType.IMGUR_IMAGE,
            MediaType.IMGUR_LINK,
            MediaType.GFYCAT,
            MediaType.REDGIFS,
            MediaType.STREAMABLE,
            MediaType.IMAGE,
            MediaType.VIDEO -> openMedia(link, mediaType)

            else -> openLink(link)
        }
    }

    private fun openLink(link: String) {
        if (linkRedirector.isPrivacyEnhancerOn) {
            val redirectLink = linkRedirector.getRedirectLink(link)
            redirectLink?.let {
                handleLink(it.original, it.redirect, it.mode)
            } ?: run {
                openBrowser(link)
            }
        } else {
            openBrowser(link)
        }
    }

    private fun handleLink(
        originalLink: HttpUrl,
        redirectLink: HttpUrl,
        mode: Redirect.RedirectMode
    ) {
        when (mode) {
            ON -> openBrowser(redirectLink.toString())
            OFF -> openBrowser(originalLink.toString())
            ALWAYS_ASK -> displayAskRedirectDialog(originalLink, redirectLink)
        }
    }

    private fun displayAskRedirectDialog(originalLink: HttpUrl, redirectLink: HttpUrl) {
        MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle(R.string.dialog_privacy_enhancer_title)
            .setMessage(
                fragment.getString(
                    R.string.dialog_privacy_enhancer_message,
                    originalLink.host,
                    redirectLink.host
                )
            )
            .setPositiveButton(R.string.dialog_yes) { _, _ -> openBrowser(redirectLink.toString()) }
            .setNegativeButton(R.string.dialog_no) { _, _ -> openBrowser(originalLink.toString()) }
            .setNeutralButton(R.string.dialog_cancel) { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }

    fun openGallery(images: List<GalleryMedia>) {
        MediaViewerFragment.newInstance(images).run {
            show(fragment.parentFragmentManager, MediaViewerFragment.TAG)
        }
    }

    fun openMedia(link: String, mediaType: MediaType) {
        MediaViewerFragment.newInstance(link, mediaType).run {
            show(fragment.parentFragmentManager, MediaViewerFragment.TAG)
        }
    }

    fun openBrowser(link: String) {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
            .build()
            .launchUrl(fragment.requireContext(), Uri.parse(link))
    }
}
