package com.cosmos.unreddit.ui.base

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.cosmos.unreddit.NavigationGraphDirections
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.ui.common.widget.RedditView
import com.cosmos.unreddit.ui.linkmenu.LinkMenuFragment
import com.cosmos.unreddit.ui.postdetails.PostDetailsFragment
import com.cosmos.unreddit.ui.postlist.PostListAdapter
import com.cosmos.unreddit.ui.postmenu.PostMenuFragment
import com.cosmos.unreddit.util.LinkHandler
import com.cosmos.unreddit.util.extension.applyWindowInsets
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
open class BaseFragment : Fragment(), PostListAdapter.PostClickListener,
    RedditView.OnLinkClickListener {

    protected open val viewModel: BaseViewModel? = null

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    private val navOptions: NavOptions by lazy {
        NavOptions.Builder()
            .setEnterAnim(R.anim.nav_enter_anim)
            .setExitAnim(R.anim.nav_exit_anim)
            .setPopEnterAnim(R.anim.nav_enter_anim)
            .setPopExitAnim(R.anim.nav_exit_anim)
            .build()
    }

    @Inject
    lateinit var linkHandler: LinkHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedCallback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            onBackPressed()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyInsets(view)
    }

    protected open fun applyInsets(view: View) {
        view.applyWindowInsets(bottom = false)
    }

    protected open fun onBackPressed() {
        onBackPressedCallback.isEnabled = false
        findNavController().navigateUp()
    }

    protected fun navigate(directions: NavDirections, navOptions: NavOptions = this.navOptions) {
        findNavController().navigate(directions, navOptions)
    }

    protected fun navigate(deepLink: Uri, navOptions: NavOptions = this.navOptions) {
        findNavController().navigate(deepLink, navOptions)
    }

    override fun onClick(post: PostEntity) {
        onClick(parentFragmentManager, post)
    }

    protected open fun onClick(fragmentManager: FragmentManager, post: PostEntity) {
        fragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.nav_enter_anim,
                R.anim.nav_exit_anim,
                R.anim.nav_enter_anim,
                R.anim.nav_exit_anim
            )
            .add(
                R.id.fragment_container,
                PostDetailsFragment.newInstance(post),
                PostDetailsFragment.TAG
            )
            .addToBackStack(null)
            .commit()
    }

    override fun onLongClick(post: PostEntity) {
        PostMenuFragment.show(parentFragmentManager, post)
    }

    override fun onMenuClick(post: PostEntity) {
        PostMenuFragment.show(parentFragmentManager, post)
    }

    override fun onImageClick(post: PostEntity) {
        viewModel?.insertPostInHistory(post.id)
        if (post.gallery.isNotEmpty()) {
            linkHandler.openGallery(post.gallery)
        } else {
            linkHandler.openMedia(post.mediaUrl, post.mediaType)
        }
    }

    override fun onVideoClick(post: PostEntity) {
        viewModel?.insertPostInHistory(post.id)
        linkHandler.openMedia(post.mediaUrl, post.mediaType)
    }

    override fun onLinkClick(post: PostEntity) {
        viewModel?.insertPostInHistory(post.id)
        onLinkClick(post.url)
    }

    override fun onLinkClick(link: String) {
        linkHandler.handleLink(link)
    }

    override fun onLinkLongClick(link: String) {
        LinkMenuFragment.show(parentFragmentManager, link)
    }

    override fun onSaveClick(post: PostEntity) {
        viewModel?.toggleSavePost(post)
    }

    open fun openSubreddit(subreddit: String) {
        navigate(NavigationGraphDirections.openSubreddit(subreddit))
    }

    open fun openUser(user: String) {
        navigate(NavigationGraphDirections.openUser(user))
    }

    open fun openRedditLink(link: String) {
        try {
            navigate(Uri.parse(link))
        } catch (e: IllegalArgumentException) {
            linkHandler.openBrowser(link)
        }
    }
}
