package com.cosmos.unreddit.ui.backup

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Precision
import coil.size.Scale
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.BackupTypeItem
import com.cosmos.unreddit.data.model.backup.BackupType
import com.cosmos.unreddit.databinding.ItemBackupTypeBinding

class BackupChoiceAdapter(
    private val onTypeSelected: (BackupType) -> Unit
) : ListAdapter<BackupTypeItem, BackupChoiceAdapter.ViewHolder>(
    BACKUP_TYPE_COMPARATOR
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemBackupTypeBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            holder.update(getItem(position))
        }
    }

    fun setChecked(backupType: BackupType?) {
        currentList
            .indexOfFirst { it.type == backupType }
            .takeIf { it != -1 }
            ?.let { position ->
                // Select item at found position
                currentList.forEachIndexed { index, backupTypeItem ->
                    backupTypeItem.selected = index == position
                    notifyItemChanged(index, backupTypeItem)
                }
            }
            ?: run {
                // Deselect all since backup type is null
                currentList.forEachIndexed { index, backupTypeItem ->
                    backupTypeItem.selected = false
                    notifyItemChanged(index, backupTypeItem)
                }
            }
    }

    inner class ViewHolder(
        private val binding: ItemBackupTypeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(backupType: BackupTypeItem) {
            binding.backupType = backupType

            update(backupType)

            binding.typeImage.load(backupType.type.icon) {
                crossfade(true)
                scale(Scale.FILL)
                precision(Precision.AUTOMATIC)
                placeholder(R.drawable.icon_reddit_placeholder)
                error(R.drawable.icon_reddit_placeholder)
                fallback(R.drawable.icon_reddit_placeholder)
            }

            itemView.setOnClickListener {
                onTypeSelected.invoke(getItem(bindingAdapterPosition).type)
            }
        }

        fun update(backupType: BackupTypeItem) {
            binding.card.isChecked = backupType.selected
        }
    }

    companion object {
        private val BACKUP_TYPE_COMPARATOR = object : DiffUtil.ItemCallback<BackupTypeItem>() {

            override fun areItemsTheSame(
                oldItem: BackupTypeItem,
                newItem: BackupTypeItem
            ): Boolean {
                return oldItem.type == newItem.type
            }

            override fun areContentsTheSame(
                oldItem: BackupTypeItem,
                newItem: BackupTypeItem
            ): Boolean {
                return oldItem.selected == newItem.selected
            }
        }
    }
}
