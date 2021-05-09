package com.cosmos.unreddit.ui.postdetails

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.request.ImageRequest
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.MediaType
import com.cosmos.unreddit.data.model.PostType
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.model.preferences.ContentPreferences
import com.cosmos.unreddit.databinding.ItemPostHeaderBinding
import com.cosmos.unreddit.ui.common.widget.RedditView
import com.cosmos.unreddit.ui.postlist.PostListAdapter
import com.cosmos.unreddit.util.extension.load

class PostAdapter(
    private val contentPreferences: ContentPreferences,
    private val postClickListener: PostListAdapter.PostClickListener,
    private val onLinkClickListener: RedditView.OnLinkClickListener? = null
) : RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    private var post: PostEntity? = null
    private var preview: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemPostHeaderBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        post?.let { holder.bind(it) }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            post?.let { holder.update(it) }
        }
    }

    override fun getItemCount(): Int = 1

    fun setPost(post: PostEntity, fromCache: Boolean) {
        var payload: Any? = null

        if (fromCache || this.post == null) {
            preview = post.preview
        } else {
            payload = post
        }

        this.post = post

        notifyItemChanged(0, payload)
    }

    inner class ViewHolder(
        private val binding: ItemPostHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostEntity) {
            binding.includePostMetrics.post = post
            binding.includePostFlairs.post = post
            binding.includePostInfo.post = post

            binding.textPostTitle.text = post.title

            binding.includePostInfo.groupCrosspost.isVisible = false
            binding.includePostInfo.textPostAuthor.apply {
                setTextColor(ContextCompat.getColor(context, post.posterType.color))
            }

            bindAwards(post)

            bindFlairs(post)

            when (post.type) {
                PostType.TEXT -> bindText(post)
                PostType.IMAGE -> {
                    bindImage(post) {
                        error(R.drawable.preview_image_fallback)
                        fallback(R.drawable.preview_image_fallback)
                    }
                    binding.imagePost.setOnClickListener { postClickListener.onImageClick(post) }
                }
                PostType.LINK -> {
                    bindImage(post) {
                        error(R.drawable.preview_link_fallback)
                        fallback(R.drawable.preview_link_fallback)
                    }
                    binding.imagePost.setOnClickListener { postClickListener.onLinkClick(post) }
                }
                PostType.VIDEO -> {
                    bindImage(post) {
                        error(R.drawable.preview_video_fallback)
                        fallback(R.drawable.preview_video_fallback)
                    }
                    binding.imagePost.setOnClickListener { postClickListener.onVideoClick(post) }
                }
            }

            binding.buttonTypeIndicator.apply {
                when {
                    post.mediaType == MediaType.REDDIT_GALLERY ||
                            post.mediaType == MediaType.IMGUR_ALBUM ||
                            post.mediaType == MediaType.IMGUR_GALLERY -> {
                        visibility = View.VISIBLE
                        setIcon(R.drawable.ic_gallery)
                    }
                    post.type == PostType.VIDEO -> {
                        visibility = View.VISIBLE
                        setIcon(R.drawable.ic_play)
                    }
                    else -> {
                        visibility = View.GONE
                    }
                }
            }

            binding.includePostMetrics.buttonMore.setOnClickListener {
                postClickListener.onMenuClick(post)
            }

            binding.includePostMetrics.buttonSave.setOnClickListener {
                postClickListener.onSaveClick(post)
            }

            post.crosspost?.let { crosspost ->
                binding.includeCrosspost.run {
                    root.isVisible = true
                    root.setOnClickListener { postClickListener.onClick(crosspost) }
                    title.text = crosspost.title
                    includePostInfo.post = crosspost
                    includePostInfo.groupCrosspost.isVisible = false
                }
            } ?: run {
                binding.includeCrosspost.root.isVisible = false
            }

            binding.includePostMetrics.buttonSave.isChecked = post.saved
        }

        fun update(post: PostEntity) {
            binding.includePostMetrics.post = post
            binding.includePostFlairs.post = post

            binding.includePostMetrics.buttonSave.isChecked = post.saved

            if (post.type == PostType.TEXT) {
                bindText(post)
            }

            bindAwards(post)

            bindFlairs(post)
        }

        private fun bindText(post: PostEntity) {
            binding.textPost.apply {
                if (post.selfRedditText.isNotEmpty()) {
                    visibility = View.VISIBLE
                    setText(post.selfRedditText)
                    setOnLinkClickListener(onLinkClickListener)
                } else {
                    visibility = View.GONE
                }
            }
        }

        private fun bindFlairs(post: PostEntity) {
            when {
                post.hasFlairs -> {
                    binding.includePostFlairs.root.visibility = View.VISIBLE
                    binding.includePostFlairs.postFlair.apply {
                        if (!post.flair.isEmpty()) {
                            visibility = View.VISIBLE

                            setFlair(post.flair)
                        } else {
                            visibility = View.GONE
                        }
                    }
                }
                post.isSelf -> {
                    binding.includePostFlairs.root.visibility = View.GONE
                }
                else -> {
                    binding.includePostFlairs.postFlair.visibility = View.GONE
                }
            }
            binding.includePostInfo.postFlair.apply {
                if (!post.authorFlair.isEmpty()) {
                    visibility = View.VISIBLE

                    setFlair(post.authorFlair)
                } else {
                    visibility = View.GONE
                }
            }
        }

        private fun bindAwards(post: PostEntity) {
            binding.awards.apply {
                if (post.totalAwards > 0) {
                    visibility = View.VISIBLE
                    setAwards(post.awards)
                } else {
                    visibility = View.GONE
                }
            }
        }

        private fun bindImage(
            post: PostEntity,
            requestBuilder: ImageRequest.Builder.() -> Unit = {}
        ) {
            binding.imagePost.apply {
                visibility = View.VISIBLE
                load(preview, !post.shouldShowPreview(contentPreferences), builder = requestBuilder)
            }
        }
    }
}
