package com.cosmos.unreddit.mediaviewer

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.cosmos.unreddit.databinding.ItemImageBinding
import com.cosmos.unreddit.model.GalleryMedia

class MediaViewerAdapter : RecyclerView.Adapter<MediaViewerAdapter.ViewHolder>() {

    private val images: MutableList<GalleryMedia> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemImageBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int {
        return images.size
    }

    fun submitData(images: List<GalleryMedia>) {
        this.images.clear()
        this.images.addAll(images)
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: ItemImageBinding
    ) : RecyclerView.ViewHolder(binding.root), View.OnTouchListener {

        fun bind(image: GalleryMedia) {
            with(binding.image) {
                Coil.imageLoader(context).enqueue(
                    ImageRequest.Builder(context).apply {
                        data(image.url)
                        crossfade(true)
                        scale(Scale.FILL)
                        precision(Precision.EXACT)
                        memoryCachePolicy(CachePolicy.READ_ONLY)
                        diskCachePolicy(CachePolicy.READ_ONLY)
                        target { drawable -> setImageDrawable(drawable) }
                    }.build()
                )
                setOnTouchListener(this@ViewHolder)
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            return if (
                event.pointerCount >= 2 ||
                view.canScrollHorizontally(1) &&
                binding.image.canScrollHorizontally(-1)
            ) {
                // Multi-touch event
                when (event.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        // Disallow RecyclerView to intercept touch events.
                        binding.image.parent.requestDisallowInterceptTouchEvent(true)
                        // Disable touch on view
                        false
                    }
                    MotionEvent.ACTION_UP -> {
                        // Allow RecyclerView to intercept touch events.
                        binding.image.parent.requestDisallowInterceptTouchEvent(false)
                        true
                    }
                    else -> true
                }
            } else {
                true
            }
        }
    }
}
