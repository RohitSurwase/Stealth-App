package com.cosmos.unreddit.util

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.databinding.ItemListContentBinding
import com.cosmos.unreddit.ui.common.PostDividerItemDecoration
import com.cosmos.unreddit.ui.loadstate.NetworkLoadStateAdapter
import com.cosmos.unreddit.util.extension.addLoadStateListener
import com.cosmos.unreddit.util.extension.applyWindowInsets

class RecyclerViewStateAdapter(val onError: () -> Unit) :
    ListAdapter<RecyclerViewStateAdapter.Page, RecyclerViewStateAdapter.ViewHolder>(COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemListContentBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindPage(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemListContentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bindPage(page: Page) {
            if (page.showItemDecoration) {
                binding.listContent.apply {
                    addItemDecoration(PostDividerItemDecoration(context))
                }
            }

            page.adapter.run {
                if (this is PagingDataAdapter<out Any, out RecyclerView.ViewHolder>) {
                    bindPaging(this)
                } else {
                    bind(this)
                }
            }
        }

        private fun bindPaging(adapter: PagingDataAdapter<out Any, out RecyclerView.ViewHolder>) {
            adapter.addLoadStateListener(binding.listContent, binding.loadingState, onError)

            binding.listContent.apply {
                applyWindowInsets(left = false, top = false, right = false)
                layoutManager = LinearLayoutManager(context)
                this.adapter = adapter.withLoadStateHeaderAndFooter(
                    header = NetworkLoadStateAdapter { adapter.retry() },
                    footer = NetworkLoadStateAdapter { adapter.retry() }
                )
            }
        }

        private fun bind(adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>) {
            binding.listContent.apply {
                applyWindowInsets(left = false, top = false, right = false)
                layoutManager = LinearLayoutManager(context)
                this.adapter = adapter
            }
        }
    }

    data class Page(
        @StringRes val title: Int,
        val adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
        val showItemDecoration: Boolean = false
    )

    companion object {
        private val COMPARATOR = object : DiffUtil.ItemCallback<Page>() {

            override fun areItemsTheSame(oldItem: Page, newItem: Page): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Page, newItem: Page): Boolean {
                return oldItem == newItem
            }
        }
    }
}
