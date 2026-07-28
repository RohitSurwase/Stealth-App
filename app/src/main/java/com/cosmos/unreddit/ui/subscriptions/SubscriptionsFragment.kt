package com.cosmos.unreddit.ui.subscriptions

import android.os.Bundle
import android.os.Parcelable
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
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.db.Subscription
import com.cosmos.unreddit.databinding.FragmentSubscriptionsBinding
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.util.SearchUtil
import com.cosmos.unreddit.util.extension.applyWindowInsets
import com.cosmos.unreddit.util.extension.hideSoftKeyboard
import com.cosmos.unreddit.util.extension.parcelable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SubscriptionsFragment : BaseFragment() {

    private var _binding: FragmentSubscriptionsBinding? = null
    private val binding get() = _binding!!

    override val viewModel: SubscriptionsViewModel by activityViewModels()

    private lateinit var subscriptionsAdapter: SubscriptionsAdapter

    // Scroll position of the list, saved when the view is destroyed and restored once the
    // subscriptions have been submitted to the adapter
    private var listState: Parcelable? = null

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
        savedInstanceState?.parcelable<Parcelable>(KEY_LIST_STATE)?.let { listState = it }
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
                    subscriptionsAdapter.submitList(subscriptions) {
                        if (subscriptions.isNotEmpty()) {
                            restoreListState()
                        }
                    }
                    if (binding.appBar.searchInput.isQueryEmpty()) {
                        binding.emptyData.isVisible = subscriptions.isEmpty()
                        binding.textEmptyData.isVisible = subscriptions.isEmpty()
                    }
                }
        }
    }

    private fun initRecyclerView() {
        subscriptionsAdapter = SubscriptionsAdapter(
            { onClick(it) },
            { showUnsubscribeDialog(it) }
        )
        binding.listSubscriptions.apply {
            applyWindowInsets(left = false, top = false, right = false)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = subscriptionsAdapter
        }
    }

    private fun saveListState() {
        _binding?.listSubscriptions?.layoutManager?.onSaveInstanceState()?.let { listState = it }
    }

    private fun restoreListState() {
        val state = listState ?: return
        listState = null
        _binding?.listSubscriptions?.layoutManager?.onRestoreInstanceState(state)
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

    private fun showUnsubscribeDialog(subscription: Subscription) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_unsubscribe_title)
            .setMessage(getString(R.string.dialog_unsubscribe_message, subscription.name))
            .setPositiveButton(R.string.dialog_yes) { _, _ ->
                viewModel.unsubscribe(subscription)
            }
            .setNegativeButton(R.string.dialog_no) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveListState()
        outState.putParcelable(KEY_LIST_STATE, listState)
    }

    override fun onDestroyView() {
        saveListState()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SubscriptionsFragment"

        private const val KEY_LIST_STATE = "list_state"
    }
}
