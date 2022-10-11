package com.cosmos.unreddit.ui.common.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.databinding.ItemListContentBinding
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.ui.common.PostDividerItemDecoration
import com.cosmos.unreddit.util.extension.applyWindowInsets
import com.cosmos.unreddit.util.extension.currentNavigationFragment

abstract class ListFragment<T : Adapter<out ViewHolder>> : BaseFragment() {

    private var _binding: ItemListContentBinding? = null
    protected val binding get() = _binding!!

    protected lateinit var adapter: T

    protected open val showItemDecoration: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ItemListContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
    }

    override fun onClick(post: PostEntity) {
        activity?.currentNavigationFragment?.let { currentFragment ->
            onClick(currentFragment.parentFragmentManager, post)
        }
    }

    protected open fun initRecyclerView() {
        adapter = createAdapter().apply {
            stateRestorationPolicy = PREVENT_WHEN_EMPTY
        }

        binding.listContent.apply {
            applyWindowInsets(left = false, top = false, right = false)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ListFragment.adapter
            if (showItemDecoration) {
                addItemDecoration(PostDividerItemDecoration(context))
            }
        }
    }

    protected abstract fun createAdapter(): T

    protected fun showRetryBar() {
        if (!binding.loadingState.infoRetry.isVisible) {
            binding.loadingState.infoRetry.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
