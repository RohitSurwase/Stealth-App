package com.cosmos.unreddit.postlist

import android.graphics.LinearGradient
import android.graphics.Shader
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Precision
import coil.size.Scale
import com.cosmos.unreddit.R
import com.cosmos.unreddit.databinding.*
import com.cosmos.unreddit.model.MediaType
import com.cosmos.unreddit.parser.ClickableMovementMethod
import com.cosmos.unreddit.parser.TextBlock
import com.cosmos.unreddit.post.PostEntity
import com.cosmos.unreddit.view.RedditView
import com.google.android.material.card.MaterialCardView

abstract class PostViewHolder(
    itemView: View,
    protected val listener: PostListAdapter.PostClickListener
) : RecyclerView.ViewHolder(itemView) {

    open fun bind(postEntity: PostEntity, onClick: (Int) -> Unit) {
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

        postInfoBinding.post = postEntity
        postMetricsBinding.post = postEntity
        postFlairsBinding.post = postEntity

        with(title) {
            text = postEntity.title
            setTextColor(postEntity.getSeenColor(title.context))
        }

        postMetricsBinding.awards.setAwards(postEntity.awards)

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

    class ImagePostViewHolder(
        private val binding: ItemPostImageBinding,
        listener: PostListAdapter.PostClickListener
    ) : PostViewHolder(binding.root, listener) {

        override fun bind(postEntity: PostEntity, onClick: (Int) -> Unit) {
            super.bind(postEntity, onClick)

            with(binding.imagePostPreview) {
                load(postEntity.preview) {
                    crossfade(true)
                    scale(Scale.FILL)
                    precision(Precision.AUTOMATIC)
                }
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

        override fun bind(postEntity: PostEntity, onClick: (Int) -> Unit) {
            super.bind(postEntity, onClick)

            with(binding.imagePostPreview) {
                load(postEntity.preview) {
                    crossfade(true)
                    scale(Scale.FILL)
                    precision(Precision.AUTOMATIC)
                }
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

        override fun bind(postEntity: PostEntity, onClick: (Int) -> Unit) {
            super.bind(postEntity, onClick)

            with(selfText) {
                if (postEntity.selfRedditText.isFirstBlockText()) {
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

        override fun bind(postEntity: PostEntity, onClick: (Int) -> Unit) {
            super.bind(postEntity, onClick)

            with(preview) {
                load(postEntity.preview) {
                    crossfade(true)
                    scale(Scale.FILL)
                    precision(Precision.AUTOMATIC)
                }
                setOnClickListener { listener.onLinkClick(postEntity) }
            }
        }
    }

    class PollPostViewHolder() {

    }
}
