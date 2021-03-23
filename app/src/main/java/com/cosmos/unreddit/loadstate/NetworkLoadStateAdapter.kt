package com.cosmos.unreddit.loadstate

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.databinding.ItemLoadStateBinding

class NetworkLoadStateAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<NetworkLoadStateAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemLoadStateBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    inner class ViewHolder(
        private val binding: ItemLoadStateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.buttonRetry.setOnClickListener { retry.invoke() }
        }

        fun bind(loadState: LoadState) {
            binding.loadingCradle.isVisible = loadState is LoadState.Loading
            binding.buttonRetry.isVisible = loadState !is LoadState.Loading
            binding.textError.isVisible = loadState !is LoadState.Loading
        }

        fun unbind() {
            binding.loadingCradle.isVisible = false
        }
    }
}
