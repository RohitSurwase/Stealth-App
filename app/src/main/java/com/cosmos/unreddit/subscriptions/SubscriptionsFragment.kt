package com.cosmos.unreddit.subscriptions

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.cosmos.unreddit.R
import com.cosmos.unreddit.UiViewModel
import com.cosmos.unreddit.databinding.FragmentSubscriptionsBinding
import com.cosmos.unreddit.search.SearchFragment
import com.cosmos.unreddit.util.hideSoftKeyboard
import com.cosmos.unreddit.util.showSoftKeyboard
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SubscriptionsFragment : Fragment() {

    private var _binding: FragmentSubscriptionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SubscriptionsViewModel by activityViewModels()
    private val uiViewModel: UiViewModel by activityViewModels()

    private lateinit var adapter: SubscriptionsAdapter

    private lateinit var onBackPressedCallback: OnBackPressedCallback

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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                showSearchInput(false)
            }
        }
        requireActivity().onBackPressedDispatcher
            .addCallback(this, onBackPressedCallback)
    }

    override fun onResume() {
        super.onResume()
        onBackPressedCallback.isEnabled = binding.appBar.searchInput.isVisible
    }

    private fun bindViewModel() {
        viewModel.subscriptions.observe(viewLifecycleOwner, adapter::submitList)
    }

    private fun initRecyclerView() {
        adapter = SubscriptionsAdapter { onClick(it) }
        binding.listSubscriptions.layoutManager = LinearLayoutManager(requireContext())
        binding.listSubscriptions.adapter = adapter
    }

    private fun initAppBar() {
        with(binding.appBar) {
            searchCard.setOnClickListener {
                showSearchInput(true)
            }
            searchInput.apply {
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
        onBackPressedCallback.isEnabled = show

        with(binding.appBar) {
            val searchInputTransition = TransitionSet().apply {
                addTransition(Fade(Fade.OUT))
                addTransition(Slide(Gravity.END))
                addTransition(Fade(Fade.IN))
                duration = 250
                addTarget(searchInput)
            }

            val appBarTransition = MaterialFadeThrough().apply {
                duration = 500
                addTarget(label)
                addTarget(searchCard)
                addListener(object : Transition.TransitionListener {
                    override fun onTransitionStart(transition: Transition) {
                        // ignore
                    }

                    override fun onTransitionEnd(transition: Transition) {
                        if (!show) {
                            searchInput.text?.clear()
                        }
                    }

                    override fun onTransitionCancel(transition: Transition) {
                        // ignore
                    }

                    override fun onTransitionPause(transition: Transition) {
                        // ignore
                    }

                    override fun onTransitionResume(transition: Transition) {
                        // ignore
                    }
                })
            }

            val transitionSet = TransitionSet().apply {
                addTransition(searchInputTransition)
                addTransition(appBarTransition)
            }

            TransitionManager.beginDelayedTransition(root, transitionSet)
            label.visibility = if (show) View.GONE else View.VISIBLE
            searchCard.visibility = if (show) View.GONE else View.VISIBLE
            searchInput.visibility = if (show) View.VISIBLE else View.GONE

            if (show) {
                searchInput.apply {
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus()
                    showSoftKeyboard()
                }
            } else {
                searchInput.apply {
                    isFocusable = false
                    isFocusableInTouchMode = false
                    hideSoftKeyboard()
                }
            }
        }
    }

    private fun showSearchFragment(query: String) {
        binding.appBar.searchInput.hideSoftKeyboard()

        findNavController().navigate(SubscriptionsFragmentDirections.search(query))

        binding.appBar.searchInput.text?.clear()
    }

    private fun onClick(subreddit: String) {
        findNavController().navigate(SubscriptionsFragmentDirections.openSubreddit(subreddit))
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            showSearchInput(false)
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
