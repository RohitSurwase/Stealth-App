package com.cosmos.unreddit.ui.subscriptions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cosmos.unreddit.R
import com.cosmos.unreddit.UiViewModel
import com.cosmos.unreddit.databinding.FragmentSubscriptionsBinding
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.ui.search.SearchFragment
import com.cosmos.unreddit.util.extension.hideSoftKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SubscriptionsFragment : BaseFragment() {

    private var _binding: FragmentSubscriptionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SubscriptionsViewModel by activityViewModels()
    private val uiViewModel: UiViewModel by activityViewModels()

    private lateinit var adapter: SubscriptionsAdapter

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
        findNavController().addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.subscriptionsFragment -> uiViewModel.setNavigationVisibility(true)
                else -> uiViewModel.setNavigationVisibility(false)
            }
        }
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
        viewModel.filteredSubscriptions.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            if (binding.appBar.searchInput.isQueryEmpty()) {
                binding.emptyData.isVisible = it.isEmpty()
                binding.textEmptyData.isVisible = it.isEmpty()
            }
        }
    }

    private fun initRecyclerView() {
        adapter = SubscriptionsAdapter { onClick(it) }
        binding.listSubscriptions.layoutManager = LinearLayoutManager(requireContext())
        binding.listSubscriptions.adapter = adapter
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
                setOnEditorActionListener { _, actionId, _ ->
                    when (actionId) {
                        EditorInfo.IME_ACTION_SEARCH -> {
                            if (text.toString().length >= SearchFragment.QUERY_MIN_LENGTH) {
                                showSearchFragment(text.toString())
                            }
                            true
                        }
                        else -> false
                    }
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

        navigate(SubscriptionsFragmentDirections.search(query))

        binding.appBar.searchInput.clear()
    }

    private fun onClick(subreddit: String) {
        navigate(SubscriptionsFragmentDirections.openSubreddit(subreddit))
    }

    override fun onBackPressed() {
        if (binding.appBar.searchInput.isVisible) {
            showSearchInput(false)
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
