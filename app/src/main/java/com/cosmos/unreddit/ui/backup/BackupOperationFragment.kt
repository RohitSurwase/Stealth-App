package com.cosmos.unreddit.ui.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import com.cosmos.unreddit.R
import com.cosmos.unreddit.databinding.FragmentBackupOperationBinding
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.util.extension.launchRepeat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BackupOperationFragment : BaseFragment() {

    private var _binding: FragmentBackupOperationBinding? = null
    private val binding get() = _binding!!

    private val backupViewModel: BackupViewModel by hiltNavGraphViewModels(R.id.backup)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupOperationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViewModel()

        binding.backupCard.setOnClickListener {
            setOperation(BackupFragment.Operation.BACKUP)
        }
        binding.restoreCard.setOnClickListener {
            setOperation(BackupFragment.Operation.RESTORE)
        }
    }

    override fun applyInsets(view: View) {
        // ignore
    }

    private fun bindViewModel() {
        launchRepeat(Lifecycle.State.STARTED) {
            launch {
                backupViewModel.operation.collect { operation ->
                    operation?.let { checkCards(it) }
                }
            }
        }
    }

    private fun setOperation(operation: BackupFragment.Operation) {
        if (backupViewModel.operation.value != operation) {
            // Reset other fields on operation change
            backupViewModel.setUri(null)
            backupViewModel.setBackupType(null)
        }
        backupViewModel.setOperation(operation)
    }

    private fun checkCards(operation: BackupFragment.Operation) {
        binding.backupCard.isChecked = operation == BackupFragment.Operation.BACKUP
        binding.restoreCard.isChecked = operation == BackupFragment.Operation.RESTORE
    }

    override fun onBackPressed() {
        // Disabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
