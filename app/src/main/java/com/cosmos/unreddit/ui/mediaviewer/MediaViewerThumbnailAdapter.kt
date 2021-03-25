package com.cosmos.unreddit.ui.mediaviewer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Precision
import coil.size.Scale
import com.cosmos.unreddit.data.model.GalleryMedia
import com.cosmos.unreddit.databinding.ItemThumbnailBinding

class MediaViewerThumbnailAdapter(
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<MediaViewerThumbnailAdapter.ViewHolder>() {

    private val images: MutableList<GalleryMedia> = mutableListOf()
    private var selected: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemThumbnailBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            holder.select(position == selected)
        }
    }

    override fun getItemCount(): Int {
        return images.size
    }

    fun submitData(images: List<GalleryMedia>) {
        this.images.clear()
        this.images.addAll(images)
        notifyDataSetChanged()
    }

    fun selectItem(position: Int) {
        val oldPosition = selected
        selected = position

        notifyItemChanged(oldPosition, position)
        notifyItemChanged(position, position)
    }

    inner class ViewHolder(
        private val binding: ItemThumbnailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(image: GalleryMedia) {
            binding.thumbnail.load(image.url) {
                crossfade(true)
                scale(Scale.FILL)
                precision(Precision.AUTOMATIC)
            }
            binding.thumbnail.setOnClickListener { onClick(absoluteAdapterPosition) }
            select(absoluteAdapterPosition == selected)
        }

        fun select(isSelected: Boolean) {
            binding.thumbnail.alpha = if (isSelected) {
                1.0F
            } else {
                0.5F
            }
        }
    }
}
