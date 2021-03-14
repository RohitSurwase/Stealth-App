package com.cosmos.unreddit.loadstate

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.api.Resource
import com.cosmos.unreddit.databinding.ItemResourceStateBinding

class ResourceStateAdapter(
    private val retry: () -> Unit
) : RecyclerView.Adapter<ResourceStateAdapter.ViewHolder>() {

    var resource: Resource<Any>? = null
        set(resource) {
            if (field != resource) {
                val oldItem = displayResourceStateAsItem(field)
                val newItem = displayResourceStateAsItem(resource)

                if (oldItem && !newItem) {
                    notifyItemRemoved(0)
                } else if (newItem && !oldItem) {
                    notifyItemInserted(0)
                } else if (oldItem && newItem) {
                    notifyItemChanged(0)
                }

                field = resource
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemResourceStateBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(resource)
    }

    override fun getItemCount(): Int = if (displayResourceStateAsItem(resource)) 1 else 0

    private fun displayResourceStateAsItem(resource: Resource<Any>?): Boolean {
        return resource != null && (resource !is Resource.Success || isEmpty(resource))
    }

    private fun isEmpty(resource: Resource<Any>?): Boolean {
        return (resource?.dataValue as? List<*>)?.isEmpty() ?: false
    }

    inner class ViewHolder(
        private val binding: ItemResourceStateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.buttonRetry.setOnClickListener { retry.invoke() }
        }

        fun bind(resource: Resource<Any>?) {
            binding.loadingCradle.isVisible = resource is Resource.Loading
            binding.buttonRetry.isVisible = resource is Resource.Error
            binding.textError.isVisible = resource is Resource.Error

            val isEmpty = isEmpty(resource)
            binding.emptyData.isVisible = isEmpty
            binding.textEmptyData.isVisible = isEmpty
        }
    }
}
