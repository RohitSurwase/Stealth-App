package com.cosmos.unreddit.postlist

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.R
import com.cosmos.unreddit.databinding.*
import com.cosmos.unreddit.model.MediaType
import com.cosmos.unreddit.model.PosterType
import com.cosmos.unreddit.parser.ClickableMovementMethod
import com.cosmos.unreddit.post.PostEntity
import com.cosmos.unreddit.preferences.ContentPreferences
import com.cosmos.unreddit.util.load
import com.cosmos.unreddit.view.AwardView

abstract class PostViewHolder(
    itemView: View,
    private val postInfoBinding: IncludePostInfoBinding,
    private val postMetricsBinding: IncludePostMetricsBinding,
    private val postFlairsBinding: IncludePostFlairsBinding,
    listener: PostListAdapter.Listener
) : RecyclerView.ViewHolder(itemView) {

    private val title = itemView.findViewById<TextView>(R.id.text_post_title)
    private val awards = itemView.findViewById<AwardView>(R.id.awards)

    init {
        itemView.apply {
            setOnClickListener {
                listener.onClick(bindingAdapterPosition)
            }
            setOnLongClickListener {
                listener.onClick(bindingAdapterPosition, true)
                return@setOnLongClickListener true
            }
        }

        postMetricsBinding.buttonMore.setOnClickListener {
            listener.onMenuClick(bindingAdapterPosition)
        }
    }

    open fun bind(
        postEntity: PostEntity,
        contentPreferences: ContentPreferences
    ) {
        postInfoBinding.post = postEntity
        postMetricsBinding.post = postEntity
        postFlairsBinding.post = postEntity

        title.apply {
            text = postEntity.title
            setTextColor(ContextCompat.getColor(context, postEntity.textColor))
        }

        awards.apply {
            if (postEntity.totalAwards > 0) {
                visibility = View.VISIBLE
                setAwards(postEntity.awards, postEntity.totalAwards)
            } else {
                visibility = View.GONE
            }
        }

        if (postEntity.posterType != PosterType.REGULAR) {
            postInfoBinding.textPostAuthor.apply {
                setTextColor(ContextCompat.getColor(context, postEntity.posterType.color))
            }
        }

        when {
            postEntity.hasFlairs -> {
                postFlairsBinding.root.visibility = View.VISIBLE
                postFlairsBinding.postFlair.apply {
                    if (!postEntity.flair.isEmpty()) {
                        visibility = View.VISIBLE

                        setFlair(postEntity.flair)
                    } else {
                        visibility = View.GONE
                    }
                }
            }
            postEntity.isSelf -> {
                postFlairsBinding.root.visibility = View.GONE
            }
            else -> {
                postFlairsBinding.postFlair.visibility = View.GONE
            }
        }
    }

    open fun update(post: PostEntity) {
        title.setTextColor(ContextCompat.getColor(title.context, post.textColor))
    }

    class ImagePostViewHolder(
        private val binding: ItemPostImageBinding,
        listener: PostListAdapter.Listener
    ) : PostViewHolder(
        binding.root,
        binding.includePostInfo,
        binding.includePostMetrics,
        binding.includePostFlairs,
        listener
    ) {

        init {
            binding.imagePostPreview.setOnClickListener {
                listener.onMediaClick(bindingAdapterPosition)
            }
        }

        override fun bind(
            postEntity: PostEntity,
            contentPreferences: ContentPreferences
        ) {
            super.bind(postEntity, contentPreferences)

            binding.imagePostPreview.load(
                postEntity.preview,
                !postEntity.shouldShowPreview(contentPreferences)
            ) {
                error(R.drawable.preview_image_fallback)
                fallback(R.drawable.preview_image_fallback)
            }

            binding.buttonTypeIndicator.apply {
                when (postEntity.mediaType) {
                    MediaType.REDDIT_GALLERY, MediaType.IMGUR_ALBUM, MediaType.IMGUR_GALLERY -> {
                        visibility = View.VISIBLE
                        setIcon(R.drawable.ic_gallery)
                    }
                    else -> {
                        visibility = View.GONE
                    }
                }
            }
        }
    }

    class VideoPostViewHolder(
        private val binding: ItemPostImageBinding,
        listener: PostListAdapter.Listener
    ) : PostViewHolder(
        binding.root,
        binding.includePostInfo,
        binding.includePostMetrics,
        binding.includePostFlairs,
        listener
    ) {

        init {
            binding.imagePostPreview.setOnClickListener {
                listener.onMediaClick(bindingAdapterPosition)
            }
        }

        override fun bind(
            postEntity: PostEntity,
            contentPreferences: ContentPreferences
        ) {
            super.bind(postEntity, contentPreferences)

            binding.imagePostPreview.load(
                postEntity.preview,
                !postEntity.shouldShowPreview(contentPreferences)
            ) {
                error(R.drawable.preview_video_fallback)
                fallback(R.drawable.preview_video_fallback)
            }

            binding.buttonTypeIndicator.apply {
                visibility = View.VISIBLE
                setIcon(R.drawable.ic_play)
            }
        }
    }

    class TextPostViewHolder(
        private val binding: ItemPostTextBinding,
        listener: PostListAdapter.Listener,
        clickableMovementMethod: ClickableMovementMethod
    ) : PostViewHolder(
        binding.root,
        binding.includePostInfo,
        binding.includePostMetrics,
        binding.includePostFlairs,
        listener
    ) {

        init {
            binding.textPostSelf.movementMethod = clickableMovementMethod
            binding.textPostSelf.setOnLongClickListener {
                listener.onClick(bindingAdapterPosition, true)
                true
            }
        }

        override fun bind(
            postEntity: PostEntity,
            contentPreferences: ContentPreferences
        ) {
            super.bind(postEntity, contentPreferences)

            binding.textPostSelf.apply {
                if (postEntity.shouldShowPreview(contentPreferences) &&
                    postEntity.previewText != null
                ) {
                    binding.textPostSelfCard.visibility = View.VISIBLE
                    setText(postEntity.previewText, false)
                    setTextColor(ContextCompat.getColor(context, postEntity.textColor))
                } else {
                    binding.textPostSelfCard.visibility = View.GONE
                }
            }
        }

        override fun update(post: PostEntity) {
            super.update(post)
            if (binding.textPostSelfCard.isVisible) {
                binding.textPostSelf.apply {
                    setTextColor(ContextCompat.getColor(context, post.textColor))
                }
            }
        }
    }

    class LinkPostViewHolder(
        private val binding: ItemPostLinkBinding,
        listener: PostListAdapter.Listener
    ) : PostViewHolder(
        binding.root,
        binding.includePostInfo,
        binding.includePostMetrics,
        binding.includePostFlairs,
        listener
    ) {

        init {
            binding.imagePostLinkPreview.setOnClickListener {
                listener.onMediaClick(bindingAdapterPosition)
            }
        }

        override fun bind(
            postEntity: PostEntity,
            contentPreferences: ContentPreferences
        ) {
            super.bind(postEntity, contentPreferences)

            binding.imagePostLinkPreview.load(
                postEntity.preview,
                !postEntity.shouldShowPreview(contentPreferences)
            ) {
                error(R.drawable.preview_link_fallback)
                fallback(R.drawable.preview_link_fallback)
            }
        }
    }

    class PollPostViewHolder() {

    }
}
