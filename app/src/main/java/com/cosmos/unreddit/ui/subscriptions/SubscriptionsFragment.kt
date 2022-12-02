package com.cosmos.unreddit.ui.subscriptions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cosmos.unreddit.NavigationGraphDirections
import com.cosmos.unreddit.databinding.FragmentSubscriptionsBinding
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.util.SearchUtil
import com.cosmos.unreddit.util.extension.applyWindowInsets
import com.cosmos.unreddit.util.extension.hideSoftKeyboard
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SubscriptionsFragment : BaseFragment() {

    private var _binding: FragmentSubscriptionsBinding? = null
    private val binding get() = _binding!!

    override val viewModel: SubscriptionsViewModel by activityViewModels()

    private lateinit var subscriptionsAdapter: SubscriptionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAppBar()
        initRecyclerView()
        bindViewModel()
    }

    override fun onResume() {
        super.onResume()
        binding.appBar.searchInput.text?.firstOrNull()?.let {
            showSearchInput(true)
        }
    }

    private fun bindViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredSubscriptions
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { subscriptions ->
                    subscriptionsAdapter.submitList(subscriptions)
                    if (binding.appBar.searchInput.isQueryEmpty()) {
                        binding.emptyData.isVisible = subscriptions.isEmpty()
                        binding.textEmptyData.isVisible = subscriptions.isEmpty()
                    }
                }
        }
    }

    private fun initRecyclerView() {
        subscriptionsAdapter = SubscriptionsAdapter { onClick(it) }
        binding.listSubscriptions.apply {
            applyWindowInsets(left = false, top = false, right = false)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = subscriptionsAdapter
        }
    }

    private fun initAppBar() {
        with(binding.appBar) {
            searchCard.setOnClickListener { showSearchInput(true) }
            cancelCard.setOnClickListener {
                showSearchInput(false)
                binding.appBar.searchInput.clear()
            }
            searchInput.apply {
                addTarget(label)
                addTarget(searchCard)
                addTarget(cancelCard)
                doOnTextChanged { text, _, _, _ ->
                    viewModel.setSearchQuery(text.toString())
                }
                setSearchActionListener {
                    handleSearchAction(it)
                }
            }
        }
    }

    private fun showSearchInput(show: Boolean) {
        binding.appBar.searchInput.show(binding.appBar.root, show) {
            with(binding.appBar) {
                label.isVisible = !show
                searchCard.isVisible = !show
                cancelCard.isVisible = show
            }
        }
    }

    private fun showSearchFragment(query: String) {
        binding.appBar.searchInput.hideSoftKeyboard()

        navigate(SubscriptionsFragmentDirections.openSearch(query))

        binding.appBar.searchInput.clear()
    }

    private fun onClick(subreddit: String) {
        navigate(NavigationGraphDirections.openSubreddit(subreddit))
    }

    private fun handleSearchAction(query: String) {
        if (SearchUtil.isQueryValid(query)) {
            showSearchFragment(query)
        }
    }

    override fun onBackPressed() {
        if (binding.appBar.searchInput.isVisible) {
            showSearchInput(false)
            binding.appBar.searchInput.clear()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SubscriptionsFragment"
    }
}
