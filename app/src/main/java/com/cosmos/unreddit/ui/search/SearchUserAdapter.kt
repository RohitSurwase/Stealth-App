package com.cosmos.unreddit.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Precision
import coil.size.Scale
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.User
import com.cosmos.unreddit.databinding.ItemSearchUserBinding

class SearchUserAdapter(
    private val listener: (String) -> Unit
) : PagingDataAdapter<User, SearchUserAdapter.UserViewHolder>(USER_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return UserViewHolder(ItemSearchUserBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position) ?: return
        holder.bind(user)
    }

    inner class UserViewHolder(
        private val binding: ItemSearchUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.user = user

            binding.userImage.load(user.icon) {
                crossfade(true)
                scale(Scale.FILL)
                precision(Precision.AUTOMATIC)
                placeholder(R.drawable.icon_reddit_placeholder)
                error(R.drawable.icon_reddit_placeholder)
                fallback(R.drawable.icon_reddit_placeholder)
            }

            itemView.setOnClickListener { listener(user.displayName) }
        }
    }

    companion object {
        private val USER_COMPARATOR = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem.displayName == newItem.displayName
            }

            override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem == newItem
            }
        }
    }
}
