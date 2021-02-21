package com.cosmos.unreddit.postdetails

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Precision
import coil.size.Scale
import com.cosmos.unreddit.databinding.ItemPostHeaderBinding
import com.cosmos.unreddit.model.PosterType
import com.cosmos.unreddit.parser.ClickableMovementMethod
import com.cosmos.unreddit.post.PostEntity
import com.cosmos.unreddit.post.PostType
import com.cosmos.unreddit.util.applyGradient

class PostAdapter(
    private val clickableMovementMethod: ClickableMovementMethod
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

    fun setPost(post: PostEntity, firstBind: Boolean) {
        this.post = post

        var payload: Any? = null

        if (firstBind) {
            preview = post.preview
        } else {
            payload = post
        }

        notifyItemChanged(0, payload)
    }

    inner class ViewHolder(
        private val binding: ItemPostHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostEntity) {
            with(binding) {
                includePostMetrics.post = post
                includePostFlairs.post = post
                includePostInfo.post = post

                textPostTitle.text = post.title

                includePostInfo.textPostAuthor.applyGradient(
                    post.author,
                    PosterType.getGradientColors(itemView.context, post.posterType)
                )

                includePostMetrics.awards.setAwards(post.awards)

                bindFlairs(post)

                when (post.type) {
                    PostType.TEXT -> bindText(post)
                    PostType.IMAGE, PostType.LINK, PostType.VIDEO -> {
                        with(imagePost) {
                            visibility = View.VISIBLE
                            load(preview) {
                                crossfade(true)
                                scale(Scale.FILL)
                                precision(Precision.AUTOMATIC)
                            }
                        }
                    }
                }
            }
        }

        fun update(post: PostEntity) {
            with(binding) {
                includePostMetrics.post = post
                includePostFlairs.post = post

                if (post.type == PostType.TEXT) {
                    bindText(post)
                }

                includePostMetrics.awards.setAwards(post.awards)

                bindFlairs(post)
            }
        }

        private fun bindText(post: PostEntity) {
            with(binding.textPost) {
                if (post.selfRedditText.isNotEmpty()) {
                    visibility = View.VISIBLE
                    setText(post.selfRedditText, clickableMovementMethod)
                } else {
                    visibility = View.GONE
                }
            }
        }

        private fun bindFlairs(post: PostEntity) {
            with(binding) {
                when {
                    post.hasFlairs() -> {
                        includePostFlairs.root.visibility = View.VISIBLE
                        with(includePostFlairs.postFlair) {
                            if (!post.flair.isEmpty()) {
                                visibility = View.VISIBLE

                                setFlair(post.flair)
                            } else {
                                visibility = View.GONE
                            }
                        }
                    }
                    post.isSelf -> {
                        includePostFlairs.root.visibility = View.GONE
                    }
                    else -> {
                        includePostFlairs.postFlair.visibility = View.GONE
                    }
                }
                with(includePostInfo.postFlair) {
                    if (!post.authorFlair.isEmpty()) {
                        visibility = View.VISIBLE

                        setFlair(post.authorFlair)
                    } else {
                        visibility = View.GONE
                    }
                }
            }
        }
    }
}
