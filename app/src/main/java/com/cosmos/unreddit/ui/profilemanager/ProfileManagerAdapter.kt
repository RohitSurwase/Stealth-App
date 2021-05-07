package com.cosmos.unreddit.ui.profilemanager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.data.model.ProfileItem
import com.cosmos.unreddit.data.model.db.Profile
import com.cosmos.unreddit.databinding.ItemNewProfileBinding
import com.cosmos.unreddit.databinding.ItemProfileBinding

class ProfileManagerAdapter(
    private val currentProfile: Profile?,
    private val profileClickListener: ProfileClickListener
) : ListAdapter<ProfileItem, RecyclerView.ViewHolder>(PROFILE_COMPARATOR) {

    interface ProfileClickListener {
        fun onProfileClick(profile: Profile)

        fun onDeleteProfileClick(profile: Profile)

        fun onNewProfileClick()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            Type.PROFILE.value -> {
                ProfileViewHolder(ItemProfileBinding.inflate(inflater, parent, false))
            }
            Type.NEW_PROFILE.value -> {
                NewProfileViewHolder(ItemNewProfileBinding.inflate(inflater, parent, false))
            }
            else -> throw IllegalArgumentException("Unknown type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            Type.PROFILE.value -> {
                val userProfile = getItem(position) as ProfileItem.UserProfile
                (holder as ProfileViewHolder).bind(userProfile.profile)
            }
            Type.NEW_PROFILE.value -> {
                // Ignore
            }
            else -> throw IllegalArgumentException("Unknown type")
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val item = getItem(position)
            if (item is ProfileItem.UserProfile) {
                (holder as ProfileViewHolder).setDeletable(item.profile)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ProfileItem.UserProfile -> Type.PROFILE.value
            is ProfileItem.NewProfile -> Type.NEW_PROFILE.value
        }
    }

    inner class ProfileViewHolder(
        private val binding: ItemProfileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(profile: Profile) {
            binding.profile = profile
            setDeletable(profile)

            itemView.setOnClickListener { profileClickListener.onProfileClick(profile) }
            binding.deleteIcon.setOnClickListener {
                profileClickListener.onDeleteProfileClick(profile)
            }
        }

        fun setDeletable(profile: Profile) {
            binding.deleteIcon.isVisible = currentProfile?.id != profile.id
        }
    }

    inner class NewProfileViewHolder(
        binding: ItemNewProfileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener { profileClickListener.onNewProfileClick() }
        }
    }

    private enum class Type(val value: Int) {
        PROFILE(0), NEW_PROFILE(1)
    }

    companion object {
        private val PROFILE_COMPARATOR = object : DiffUtil.ItemCallback<ProfileItem>() {
            override fun areItemsTheSame(oldItem: ProfileItem, newItem: ProfileItem): Boolean {
                return if (oldItem is ProfileItem.UserProfile && newItem is ProfileItem.UserProfile) {
                    oldItem.profile.id == newItem.profile.id
                } else {
                    oldItem is ProfileItem.NewProfile && newItem is ProfileItem.NewProfile
                }
            }

            override fun areContentsTheSame(oldItem: ProfileItem, newItem: ProfileItem): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(oldItem: ProfileItem, newItem: ProfileItem): Any? {
                return newItem
            }
        }
    }
}
