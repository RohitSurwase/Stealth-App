package com.cosmos.unreddit.ui.profile

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.Comment
import com.cosmos.unreddit.data.model.PostType
import com.cosmos.unreddit.data.model.SavedItem
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.model.preferences.ContentPreferences
import com.cosmos.unreddit.databinding.ItemPostImageBinding
import com.cosmos.unreddit.databinding.ItemPostLinkBinding
import com.cosmos.unreddit.databinding.ItemPostTextBinding
import com.cosmos.unreddit.databinding.ItemUserCommentBinding
import com.cosmos.unreddit.ui.common.widget.RedditView
import com.cosmos.unreddit.ui.postlist.PostListAdapter
import com.cosmos.unreddit.ui.postlist.PostViewHolder
import com.cosmos.unreddit.ui.user.UserCommentsAdapter
import com.cosmos.unreddit.util.ClickableMovementMethod
import com.cosmos.unreddit.util.DateUtil
import com.cosmos.unreddit.util.extension.blurText

class ProfileSavedAdapter(
    context: Context,
    private val postClickListener: PostListAdapter.PostClickListener,
    private val commentClickListener: UserCommentsAdapter.CommentClickListener,
    private val onLinkClickListener: RedditView.OnLinkClickListener? = null,
) : ListAdapter<SavedItem, RecyclerView.ViewHolder>(SAVED_COMPARATOR) {

    private val colorPrimary by lazy {
        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary))
    }

    private val clickableMovementMethod = ClickableMovementMethod(
        object : ClickableMovementMethod.OnClickListener {
            override fun onLinkClick(link: String) {
                onLinkClickListener?.onLinkClick(link)
            }

            override fun onLinkLongClick(link: String) {
                onLinkClickListener?.onLinkLongClick(link)
            }

            override fun onClick() {
                // ignore
            }

            override fun onLongClick() {
                // ignore
            }
        }
    )

    var contentPreferences: ContentPreferences = ContentPreferences(
        showNsfw = false,
        showNsfwPreview = false,
        showSpoilerPreview = false
    )
        set(value) {
            if (field.showNsfwPreview != value.showNsfwPreview ||
                field.showSpoilerPreview != value.showSpoilerPreview
            ) {
                field = value
                notifyDataSetChanged()
            }
        }

    private val listener = object : PostListAdapter.Listener {
        override fun onClick(position: Int, isLong: Boolean) {
            getPost(position)?.let {
                if (isLong) {
                    postClickListener.onLongClick(it)
                } else {
                    postClickListener.onClick(it)
                }
            }
        }

        override fun onMediaClick(position: Int) {
            getPost(position)?.let {
                when (it.type) {
                    PostType.IMAGE -> postClickListener.onImageClick(it)
                    PostType.LINK -> postClickListener.onLinkClick(it)
                    PostType.VIDEO -> postClickListener.onVideoClick(it)
                    else -> {
                        // ignore
                    }
                }
            }
        }

        override fun onMenuClick(position: Int) {
            getPost(position)?.let {
                postClickListener.onMenuClick(it)
            }
        }

        override fun onSaveClick(position: Int) {
            getPost(position)?.let {
                postClickListener.onSaveClick(it)
                it.saved = !it.saved
                notifyItemChanged(position, it)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            // Text post
            PostType.TEXT.value -> PostViewHolder.TextPostViewHolder(
                ItemPostTextBinding.inflate(inflater, parent, false),
                listener,
                clickableMovementMethod
            )
            // Image post
            PostType.IMAGE.value -> PostViewHolder.ImagePostViewHolder(
                ItemPostImageBinding.inflate(inflater, parent, false),
                listener
            )
            // Video post
            PostType.VIDEO.value -> PostViewHolder.VideoPostViewHolder(
                ItemPostImageBinding.inflate(inflater, parent, false),
                listener
            )
            // Link post
            PostType.LINK.value -> PostViewHolder.LinkPostViewHolder(
                ItemPostLinkBinding.inflate(inflater, parent, false),
                listener
            )
            COMMENT_TYPE -> CommentViewHolder(ItemUserCommentBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Unknown type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            // Text post
            PostType.TEXT.value -> (holder as PostViewHolder.TextPostViewHolder).bind(
                getPost(position)!!,
                contentPreferences
            )
            // Image post
            PostType.IMAGE.value -> (holder as PostViewHolder.ImagePostViewHolder).bind(
                getPost(position)!!,
                contentPreferences
            )
            // Video post
            PostType.VIDEO.value -> (holder as PostViewHolder.VideoPostViewHolder).bind(
                getPost(position)!!,
                contentPreferences
            )
            // Link post
            PostType.LINK.value -> (holder as PostViewHolder.LinkPostViewHolder).bind(
                getPost(position)!!,
                contentPreferences
            )
            COMMENT_TYPE -> (holder as CommentViewHolder).bind(
                (getItem(position) as SavedItem.Comment).comment
            )
            else -> throw IllegalArgumentException("Unknown type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is SavedItem.Post -> item.post.type.value
            is SavedItem.Comment -> COMMENT_TYPE
            else -> -1
        }
    }

    private fun getPost(position: Int): PostEntity? {
        return (getItem(position) as? SavedItem.Post)?.post
    }

    private inner class CommentViewHolder(
        private val binding: ItemUserCommentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: Comment.CommentEntity) {
            binding.comment = comment
            binding.includeItemComment.comment = comment

            with(comment) {
                binding.includeItemComment.commentScore.blurText(scoreHidden)

                binding.includeItemComment.commentDate.run {
                    val timeDifference = DateUtil.getTimeDifference(context, created)
                    text = if (edited > -1) {
                        val editedTimeDifference = DateUtil.getTimeDifference(
                            context,
                            edited,
                            false
                        )
                        context.getString(
                            R.string.comment_date_edited,
                            timeDifference,
                            editedTimeDifference
                        )
                    } else {
                        timeDifference
                    }
                }

                binding.includeItemComment.commentColorIndicator.run {
                    visibility = View.VISIBLE
                    backgroundTintList = colorPrimary
                }

                binding.includeItemComment.commentFlair.run {
                    if (!flair.isEmpty()) {
                        visibility = View.VISIBLE

                        setFlair(flair)
                    } else {
                        visibility = View.GONE
                    }
                }

                binding.includeItemComment.commentAwards.run {
                    if (awards.isNotEmpty()) {
                        visibility = View.VISIBLE

                        setAwards(awards, totalAwards)
                    } else {
                        visibility = View.GONE
                    }
                }

            }

            binding.includeItemComment.commentBody.run {
                setText(comment.body)
                setOnLinkClickListener(onLinkClickListener)
                setOnClickListener { commentClickListener.onClick(comment) }
                setOnLongClickListener {
                    commentClickListener.onLongClick(comment)
                    true
                }
            }

            itemView.run {
                setOnClickListener { commentClickListener.onClick(comment) }
                setOnLongClickListener {
                    commentClickListener.onLongClick(comment)
                    true
                }
            }
        }
    }

    companion object {
        private const val COMMENT_TYPE = 99

        private val SAVED_COMPARATOR = object : DiffUtil.ItemCallback<SavedItem>() {
            override fun areItemsTheSame(oldItem: SavedItem, newItem: SavedItem): Boolean {
                return if (oldItem is SavedItem.Post && newItem is SavedItem.Post) {
                    oldItem.post.id == newItem.post.id
                } else if (oldItem is SavedItem.Comment && newItem is SavedItem.Comment) {
                    oldItem.comment.name == newItem.comment.name
                } else {
                    false
                }
            }

            override fun areContentsTheSame(oldItem: SavedItem, newItem: SavedItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
