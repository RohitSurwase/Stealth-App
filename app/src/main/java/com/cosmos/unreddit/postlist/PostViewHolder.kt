package com.cosmos.unreddit.postlist

import android.graphics.LinearGradient
import android.graphics.Shader
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.transform.BlurTransformation
import com.cosmos.unreddit.R
import com.cosmos.unreddit.databinding.*
import com.cosmos.unreddit.model.MediaType
import com.cosmos.unreddit.parser.ClickableMovementMethod
import com.cosmos.unreddit.parser.TextBlock
import com.cosmos.unreddit.post.PostEntity
import com.cosmos.unreddit.preferences.ContentPreferences
import com.cosmos.unreddit.view.AwardView
import com.cosmos.unreddit.view.RedditView
import com.google.android.material.card.MaterialCardView

abstract class PostViewHolder(
    itemView: View,
    protected val listener: PostListAdapter.PostClickListener
) : RecyclerView.ViewHolder(itemView) {

    open fun bind(
        postEntity: PostEntity,
        contentPreferences: ContentPreferences,
        onClick: (Int) -> Unit
    ) {
        val postInfo = itemView.findViewById<View>(R.id.include_post_info)
        val postInfoBinding =
            DataBindingUtil.bind<IncludePostInfoBinding>(postInfo) ?: return

        val postMetrics = itemView.findViewById<View>(R.id.include_post_metrics)
        val postMetricsBinding =
            DataBindingUtil.bind<IncludePostMetricsBinding>(postMetrics) ?: return

        val postFlairs = itemView.findViewById<View>(R.id.include_post_flairs)
        val postFlairsBinding =
            DataBindingUtil.bind<IncludePostFlairsBinding>(postFlairs) ?: return

        val title = itemView.findViewById<TextView>(R.id.text_post_title)
        val awards = itemView.findViewById<AwardView>(R.id.awards)

        postInfoBinding.post = postEntity
        postMetricsBinding.post = postEntity
        postFlairsBinding.post = postEntity

        with(title) {
            text = postEntity.title
            setTextColor(postEntity.getSeenColor(title.context))
        }

        with(awards) {
            if (postEntity.totalAwards > 0) {
                visibility = View.VISIBLE
                setAwards(postEntity.awards, postEntity.totalAwards)
            } else {
                visibility = View.GONE
            }
        }

        with(postInfoBinding.textPostAuthor) {
            val width = paint.measureText(postEntity.author)
            val gradientShader = LinearGradient(0F, 0F, width, textSize,
                postEntity.getAuthorGradientColors(context), null, Shader.TileMode.CLAMP)
            paint.shader = gradientShader
        }

        when {
            postEntity.hasFlairs() -> {
                postFlairs.visibility = View.VISIBLE
                with(postFlairsBinding.postFlair) {
                    if (!postEntity.flair.isEmpty()) {
                        visibility = View.VISIBLE

                        setFlair(postEntity.flair)
                    } else {
                        visibility = View.GONE
                    }
                }
            }
            postEntity.isSelf -> {
                postFlairs.visibility = View.GONE
            }
            else -> {
                postFlairsBinding.postFlair.visibility = View.GONE
            }
        }

        with(itemView) {
            setOnClickListener {
                onClick(bindingAdapterPosition)
                listener.onClick(postEntity)
            }
            setOnLongClickListener {
                listener.onLongClick(postEntity)
                return@setOnLongClickListener true
            }
        }
    }

    protected fun ImageView.load(
        post: PostEntity,
        contentPreferences: ContentPreferences,
        builder: ImageRequest.Builder.() -> Unit = {}
    ) {
        val request = ImageRequest.Builder(context)
            .data(post.preview)
            .target(this)
            .crossfade(true)
            .scale(Scale.FILL)
            .precision(Precision.AUTOMATIC)
            .apply(builder)
            .apply {
                if ((post.isOver18 && !contentPreferences.showNsfwPreview) ||
                    (post.isSpoiler && !contentPreferences.showSpoilerPreview)
                ) {
                    transformations(BlurTransformation(context, BLUR_RADIUS, BLUR_SAMPLING))
                }
            }
            .build()
        context.imageLoader.enqueue(request)
    }

    class ImagePostViewHolder(
        private val binding: ItemPostImageBinding,
        listener: PostListAdapter.PostClickListener
    ) : PostViewHolder(binding.root, listener) {

        override fun bind(
            postEntity: PostEntity,
            contentPreferences: ContentPreferences,
            onClick: (Int) -> Unit
        ) {
            super.bind(postEntity, contentPreferences, onClick)

            with(binding.imagePostPreview) {
                load(postEntity, contentPreferences)

                setOnClickListener { listener.onImageClick(postEntity) }
            }

            with(binding.buttonTypeIndicator) {
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
        listener: PostListAdapter.PostClickListener
    ) : PostViewHolder(binding.root, listener) {

        override fun bind(
            postEntity: PostEntity,
            contentPreferences: ContentPreferences,
            onClick: (Int) -> Unit
        ) {
            super.bind(postEntity, contentPreferences, onClick)

            with(binding.imagePostPreview) {
                load(postEntity, contentPreferences)

                setOnClickListener { listener.onVideoClick(postEntity) }
            }

            with(binding.buttonTypeIndicator) {
                visibility = View.VISIBLE
                setIcon(R.drawable.ic_play)
            }
        }
    }

    class TextPostViewHolder(
        binding: ItemPostTextBinding,
        listener: PostListAdapter.PostClickListener,
        private val clickableMovementMethod: ClickableMovementMethod
    ) : PostViewHolder(binding.root, listener) {
        private val selfText: RedditView = binding.textPostSelf
        private val selfTextCard: MaterialCardView = binding.textPostSelfCard

        override fun bind(
            postEntity: PostEntity,
            contentPreferences: ContentPreferences,
            onClick: (Int) -> Unit
        ) {
            super.bind(postEntity, contentPreferences, onClick)

            with(selfText) {
                if (postEntity.selfRedditText.isFirstBlockText() &&
                    (!postEntity.isOver18 || contentPreferences.showNsfwPreview) &&
                    (!postEntity.isSpoiler || contentPreferences.showSpoilerPreview)
                ) {
                    selfTextCard.visibility = View.VISIBLE
                    setPreviewText(
                        postEntity.selfRedditText.blocks[0].block as TextBlock,
                        clickableMovementMethod
                    )
                    setTextColor(postEntity.getSeenColor(context))
                } else {
                    selfTextCard.visibility = View.GONE
                }
            }
        }
    }

    class LinkPostViewHolder(
        binding: ItemPostLinkBinding,
        listener: PostListAdapter.PostClickListener
    ) : PostViewHolder(binding.root, listener) {
        private val preview: ImageView = binding.imagePostLinkPreview

        override fun bind(
            postEntity: PostEntity,
            contentPreferences: ContentPreferences,
            onClick: (Int) -> Unit
        ) {
            super.bind(postEntity, contentPreferences, onClick)

            with(preview) {
                load(postEntity, contentPreferences)

                setOnClickListener { listener.onLinkClick(postEntity) }
            }
        }
    }

    class PollPostViewHolder() {

    }

    companion object {
        private const val BLUR_RADIUS = 25F
        private const val BLUR_SAMPLING = 4F
    }
}
