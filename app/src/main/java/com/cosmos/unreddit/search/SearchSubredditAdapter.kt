package com.cosmos.unreddit.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.databinding.ItemSearchSubredditBinding
import com.cosmos.unreddit.subreddit.SubredditEntity
import com.cosmos.unreddit.util.loadSubredditIcon

class SearchSubredditAdapter(
    private val listener: (String) -> Unit
) : PagingDataAdapter<SubredditEntity, SearchSubredditAdapter.SubredditViewHolder>(
    SUBREDDIT_COMPARATOR
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubredditViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return SubredditViewHolder(ItemSearchSubredditBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: SubredditViewHolder, position: Int) {
        val subreddit = getItem(position) ?: return
        holder.bind(subreddit)
    }

    inner class SubredditViewHolder(
        private val binding: ItemSearchSubredditBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(subreddit: SubredditEntity) {
            // TODO: Add NSFW flair next to name when needed
            binding.subreddit = subreddit

            binding.subredditImage.loadSubredditIcon(subreddit.icon)

            itemView.setOnClickListener { listener(subreddit.displayName) }
        }
    }

    companion object {
        private val SUBREDDIT_COMPARATOR = object : DiffUtil.ItemCallback<SubredditEntity>() {
            override fun areItemsTheSame(
                oldItem: SubredditEntity,
                newItem: SubredditEntity
            ): Boolean {
                return oldItem.displayName == newItem.displayName
            }

            override fun areContentsTheSame(
                oldItem: SubredditEntity,
                newItem: SubredditEntity
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
