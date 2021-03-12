package com.cosmos.unreddit.postlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.databinding.ItemPostImageBinding
import com.cosmos.unreddit.databinding.ItemPostLinkBinding
import com.cosmos.unreddit.databinding.ItemPostTextBinding
import com.cosmos.unreddit.post.PostEntity
import com.cosmos.unreddit.post.PostType
import com.cosmos.unreddit.preferences.ContentPreferences
import com.cosmos.unreddit.view.RedditView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PostListAdapter(
    private val repository: PostListRepository,
    private val listener: PostClickListener,
    private val onLinkClickListener: RedditView.OnLinkClickListener? = null
) : PagingDataAdapter<PostEntity, RecyclerView.ViewHolder>(POST_COMPARATOR) {

    interface PostClickListener {
        fun onClick(post: PostEntity)

        fun onLongClick(post: PostEntity)

        fun onMenuClick(post: PostEntity)

        fun onImageClick(post: PostEntity)

        fun onVideoClick(post: PostEntity)

        fun onLinkClick(post: PostEntity)
    }

    private var contentPreferences: ContentPreferences = ContentPreferences(
        showNsfw = false,
        showNsfwPreview = false,
        showSpoilerPreview = false
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            // Text post
            PostType.TEXT.value -> PostViewHolder.TextPostViewHolder(
                ItemPostTextBinding.inflate(inflater, parent, false),
                listener,
                onLinkClickListener
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
            else -> throw IllegalArgumentException("Unknown type $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position)?.type?.value ?: -1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position) ?: return

        when (getItemViewType(position)) {
            // Text post
            PostType.TEXT.value -> (holder as PostViewHolder.TextPostViewHolder).bind(
                item,
                contentPreferences,
                this::onClick
            )
            // Image post
            PostType.IMAGE.value -> (holder as PostViewHolder.ImagePostViewHolder).bind(
                item,
                contentPreferences,
                this::onClick
            )
            // Video post
            PostType.VIDEO.value -> (holder as PostViewHolder.VideoPostViewHolder).bind(
                item,
                contentPreferences,
                this::onClick
            )
            // Link post
            PostType.LINK.value -> (holder as PostViewHolder.LinkPostViewHolder).bind(
                item,
                contentPreferences,
                this::onClick
            )
            else -> throw IllegalArgumentException("Unknown type")
        }
    }

    private fun onClick(position: Int) {
        getItem(position)?.let {
            it.seen = true
            insertPostInHistory(it.id)
            notifyItemChanged(position, it) // TODO: Check with history in viewmodels
        }
    }

    private fun insertPostInHistory(id: String) {
        GlobalScope.launch {
            repository.insertPostInHistory(id)
        }
    }

    fun setContentPreferences(contentPreferences: ContentPreferences) {
        if (this.contentPreferences.showNsfwPreview != contentPreferences.showNsfwPreview ||
            this.contentPreferences.showSpoilerPreview != contentPreferences.showSpoilerPreview
        ) {
            this.contentPreferences = contentPreferences
            notifyDataSetChanged()
        }
    }

    companion object {
        private val POST_COMPARATOR = object : DiffUtil.ItemCallback<PostEntity>() {
            override fun areItemsTheSame(oldItem: PostEntity, newItem: PostEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PostEntity, newItem: PostEntity): Boolean {
                return oldItem == newItem
            }
        }
    }
}
