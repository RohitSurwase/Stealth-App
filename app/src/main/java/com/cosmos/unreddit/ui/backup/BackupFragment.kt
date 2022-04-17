package com.cosmos.unreddit.ui.backup

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.Resource
import com.cosmos.unreddit.data.model.backup.BackupType
import com.cosmos.unreddit.data.model.backup.Profile
import com.cosmos.unreddit.databinding.FragmentBackupBinding
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.util.extension.applyWindowInsets
import com.cosmos.unreddit.util.extension.launchRepeat
import com.cosmos.unreddit.util.extension.nextPage
import com.cosmos.unreddit.util.extension.previousPage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BackupFragment : BaseFragment() {

    private var _binding: FragmentBackupBinding? = null
    private val binding get() = _binding!!

    private val backupViewModel: BackupViewModel by hiltNavGraphViewModels(R.id.backup)

    private lateinit var backupStateAdapter: BackupStateAdapter

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            backupViewModel.setPage(position)
        }
    }

    private val currentStep: Class<out BaseFragment>?
        get() = backupStateAdapter.getStep(backupViewModel.page.value)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewPager()
        bindViewModel()

        binding.appBar.backCard.setOnClickListener { onBackPressed() }
        binding.previousButton.setOnClickListener { binding.viewPager.previousPage() }
        binding.nextButton.setOnClickListener { handleNextButtonClick() }
    }

    override fun applyInsets(view: View) {
        view.applyWindowInsets()
    }

    private fun initViewPager() {
        backupStateAdapter = BackupStateAdapter(this)

        binding.viewPager.apply {
            adapter = backupStateAdapter
            isUserInputEnabled = false
            registerOnPageChangeCallback(onPageChangeCallback)
        }
    }

    private fun bindViewModel() {
        launchRepeat(Lifecycle.State.STARTED) {
            launch {
                backupViewModel.operation.collect { operation ->
                    backupStateAdapter.operation = operation
                    backupViewModel.page.value.let { page ->
                        // Operation has changed or has been retrieved on configuration change
                        if (page != 0) {
                            // Page is not the first, so configuration has changed (e.g. rotation)
                            // ViewPager position needs to be restored
                            binding.viewPager.setCurrentItem(page, false)
                        }
                    }
                }
            }

            launch {
                combine(
                    backupViewModel.page,
                    backupViewModel.operation,
                    backupViewModel.backupType,
                    backupViewModel.chosenUri,
                    backupViewModel.operationStatus
                ) { _, operation, type, uri, status ->
                    setUpButtons(operation, type, uri, status)
                }.collect()
            }
        }
    }

    private fun setUpButtons(
        operation: Operation?,
        backupType: BackupType?,
        uri: Uri?,
        status: Resource<List<Profile>>
    ) {
        val step = currentStep

        binding.nextButton.apply {
            isEnabled = canGoNext(operation, backupType, uri, status)
            text = when (step) {
                BackupLocationFragment::class.java -> getString(R.string.backup_button_lets_go)
                BackupLoadingFragment::class.java -> getString(R.string.backup_button_finish)
                else -> getString(R.string.backup_button_next)
            }
        }

        binding.previousButton.isVisible = step != BackupOperationFragment::class.java &&
                step != BackupLoadingFragment::class.java
    }

    private fun canGoNext(
        operation: Operation?,
        backupType: BackupType?,
        uri: Uri?,
        status: Resource<List<Profile>>
    ): Boolean {
        if (operation == null) return false

        return when (currentStep) {
            BackupOperationFragment::class.java -> true
            BackupChoiceFragment::class.java -> backupType != null
            BackupLocationFragment::class.java -> uri != null
            BackupLoadingFragment::class.java -> {
                status is Resource.Success || status is Resource.Error
            }
            else -> false
        }
    }

    private fun handleNextButtonClick() {
        val nextStep = backupStateAdapter.getStep(backupViewModel.page.value + 1)
        if (nextStep == BackupLoadingFragment::class.java) {
            backupViewModel.runOperation()
        }

        if (currentStep != BackupLoadingFragment::class.java) {
            binding.viewPager.nextPage()
        } else {
            findNavController().navigateUp()
        }
    }

    override fun onBackPressed() {
        if (currentStep != BackupLoadingFragment::class.java) {
            super.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewPager.unregisterOnPageChangeCallback(onPageChangeCallback)
        _binding = null
    }

    enum class Operation {
        BACKUP, RESTORE
    }
}
