package com.cosmos.unreddit.ui.postlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.data.model.db.Profile
import com.cosmos.unreddit.databinding.ItemProfileHomeBinding

class ProfileAdapter(
    val onClickListener: (Profile) -> Unit
) : ListAdapter<Profile, ProfileAdapter.ViewHolder>(PROFILE_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemProfileHomeBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemProfileHomeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(profile: Profile) {
            binding.profile = profile
            binding.profileAvatar.setOnClickListener { onClickListener.invoke(profile) }
        }
    }

    companion object {
        private val PROFILE_COMPARATOR = object : DiffUtil.ItemCallback<Profile>() {
            override fun areItemsTheSame(oldItem: Profile, newItem: Profile): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Profile, newItem: Profile): Boolean {
                return oldItem == newItem
            }
        }
    }
}
