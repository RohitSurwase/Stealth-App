package com.cosmos.unreddit.ui.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import coil.load
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.Resource
import com.cosmos.unreddit.data.model.backup.BackupType
import com.cosmos.unreddit.data.model.backup.Profile
import com.cosmos.unreddit.databinding.FragmentBackupLoadingBinding
import com.cosmos.unreddit.ui.backup.BackupFragment.Operation.BACKUP
import com.cosmos.unreddit.ui.backup.BackupFragment.Operation.RESTORE
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.util.extension.launchRepeat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BackupLoadingFragment : BaseFragment() {

    private var _binding: FragmentBackupLoadingBinding? = null
    private val binding get() = _binding!!

    private val backupViewModel: BackupViewModel by hiltNavGraphViewModels(R.id.backup)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupLoadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViewModel()
    }

    private fun bindViewModel() {
        launchRepeat(Lifecycle.State.STARTED) {
            launch {
                backupViewModel.operationStatus.collect { status ->
                    handleStatus(status)
                }
            }
        }
    }

    private fun handleStatus(status: Resource<List<Profile>>) {
        binding.loadingCradle.isVisible = status is Resource.Loading
        when (status) {
            is Resource.Success -> {
                binding.imageResult.apply {
                    isVisible = true
                    load(R.drawable.image_completed)
                }
                setResultText(true)
            }
            is Resource.Error -> {
                binding.imageResult.apply {
                    isVisible = true
                    load(R.drawable.image_failed)
                }
                setResultText(false)
            }
            is Resource.Loading -> {
                // ignore
            }
        }
    }

    private fun setResultText(success: Boolean) {
        backupViewModel.operation.value?.let { operation ->
            binding.textResult.apply {
                isVisible = true
                text = when (operation) {
                    BACKUP -> {
                        if (success) {
                            getString(R.string.backup_loading_success_backup)
                        } else {
                            getString(R.string.backup_loading_failed_backup)
                        }
                    }
                    RESTORE -> backupViewModel.backupType.value?.let { backupType ->
                        getRestoreResultText(backupType, success)
                    }
                }
            }
        }
    }

    private fun getRestoreResultText(backupType: BackupType, success: Boolean): String {
        return when (backupType) {
            BackupType.STEALTH -> {
                if (success) {
                    getString(R.string.backup_loading_success_restore_stealth)
                } else {
                    getString(R.string.backup_loading_failed_restore_stealth)
                }
            }
            BackupType.REDDIT -> {
                if (success) {
                    getString(R.string.backup_loading_success_restore_reddit)
                } else {
                    getString(R.string.backup_loading_failed_restore_reddit)
                }
            }
        }
    }

    override fun onBackPressed() {
        // Disabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
