package com.cosmos.unreddit.ui.backup

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.navigation.navGraphViewModels
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.backup.BackupType
import com.cosmos.unreddit.databinding.FragmentBackupLocationBinding
import com.cosmos.unreddit.ui.backup.BackupFragment.Operation.BACKUP
import com.cosmos.unreddit.ui.backup.BackupFragment.Operation.RESTORE
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.util.DateUtil
import com.cosmos.unreddit.util.extension.getFilename
import com.cosmos.unreddit.util.extension.launchRepeat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Date

@AndroidEntryPoint
class BackupLocationFragment : BaseFragment() {

    private var _binding: FragmentBackupLocationBinding? = null
    private val binding get() = _binding!!

    private val backupViewModel: BackupViewModel by navGraphViewModels(R.id.backup)

    private val openDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
        this::setUri
    )

    private val createDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument(),
        this::setUri
    )

    private val filename: String
        get() = getString(R.string.app_name) +
                "_" +
                DateUtil.getFormattedDate(getString(R.string.file_date_format), Date()) +
                ".json"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViewModel()

        binding.pickButton.setOnClickListener {
            backupViewModel.operation.value?.let { operation ->
                pickFile(operation)
            }
        }
    }

    private fun bindViewModel() {
        launchRepeat(Lifecycle.State.STARTED) {
            launch {
                combine(
                    backupViewModel.operation,
                    backupViewModel.backupType
                ) { operation, type ->
                    setExplanationText(operation, type)
                }.collect()
            }

            launch {
                backupViewModel.chosenUri.collect { uri ->
                    binding.textFilename.text = uri?.getFilename(requireContext())
                }
            }
        }
    }

    private fun setExplanationText(operation: BackupFragment.Operation?, backupType: BackupType?) {
        binding.textExplanation.text = when (operation) {
            BACKUP -> getString(R.string.backup_location_explanation_backup)
            RESTORE -> backupType?.let { getRestoreExplanationText(it) }
            else -> ""
        }
    }

    private fun getRestoreExplanationText(backupType: BackupType): String {
        return when (backupType) {
            BackupType.STEALTH -> getString(R.string.backup_location_explanation_restore_stealth)
            BackupType.REDDIT -> getString(R.string.backup_location_explanation_restore_reddit)
        }
    }

    private fun pickFile(operation: BackupFragment.Operation) {
        when (operation) {
            BACKUP -> {
                createDocument.launch(filename)
            }
            else -> {
                val type = backupViewModel.backupType.value?.mime ?: arrayOf("*/*")
                openDocument.launch(type)
            }
        }
    }

    private fun setUri(uri: Uri?) {
        backupViewModel.setUri(uri)
    }

    override fun onBackPressed() {
        // Disabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
