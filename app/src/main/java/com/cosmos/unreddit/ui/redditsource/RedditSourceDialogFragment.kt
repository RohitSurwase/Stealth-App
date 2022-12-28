package com.cosmos.unreddit.ui.redditsource

import android.app.Dialog
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.preferences.DataPreferences
import com.cosmos.unreddit.databinding.FragmentRedditSourceBinding
import com.cosmos.unreddit.util.LinkValidator
import com.cosmos.unreddit.util.extension.doAndDismiss
import com.cosmos.unreddit.util.extension.serializable
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RedditSourceDialogFragment : DialogFragment(), OnShowListener {

    private var _binding: FragmentRedditSourceBinding? = null
    private val binding get() = _binding!!

    private lateinit var source: DataPreferences.RedditSource
    private var instance: String? = null
    private lateinit var instances: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.run {
            source = serializable(KEY_SOURCE) ?: DataPreferences.RedditSource.REDDIT
            instance = serializable(KEY_INSTANCE)
            instances = getStringArrayList(KEY_INSTANCES) ?: emptyList()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentRedditSourceBinding.inflate(requireActivity().layoutInflater)

        initView()
        initSpinner()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_reddit_source_title)
            .setView(binding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                // Ignore
            }
            .setNeutralButton(R.string.dialog_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()
            .apply {
                setOnShowListener(this@RedditSourceDialogFragment)
            }
    }

    private fun initView() {
        binding.run {
            val isReddit = source == DataPreferences.RedditSource.REDDIT
            radioReddit.isChecked = isReddit
            radioTeddit.isChecked = !isReddit

            listInstances.isVisible = !isReddit

            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                listInstances.isVisible = checkedId == R.id.radio_teddit
            }
        }
    }

    private fun initSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, instances)

        val savedInstance = instance.orEmpty().ifEmpty { adapter.getItem(0) }

        binding.textListInstances.run {
            setAdapter(adapter)
            setText(savedInstance, false)
        }
    }

    private fun save() {
        val source = when (binding.radioGroup.checkedRadioButtonId) {
            R.id.radio_reddit -> DataPreferences.RedditSource.REDDIT
            R.id.radio_teddit -> DataPreferences.RedditSource.TEDDIT
            else -> DataPreferences.RedditSource.REDDIT
        }
        val instance = binding.textListInstances.text.toString()
        val linkValidator = LinkValidator(instance)

        var errorMessage: String? = null

        if (source == DataPreferences.RedditSource.TEDDIT) {
            errorMessage = when {
                instance.isBlank() -> getString(R.string.instance_empty_error)
                !linkValidator.isValid -> getString(R.string.instance_invalid_error)
                else -> null
            }
        }

        if (errorMessage != null) {
            binding.listInstances.error = errorMessage
            return
        }

        doAndDismiss {
            setFragmentResult(
                REQUEST_KEY_SOURCE,
                bundleOf(
                    KEY_SOURCE to source,
                    KEY_INSTANCE to linkValidator.validUrl?.host?.ifEmpty { "" }
                )
            )
        }
    }

    override fun onShow(dialog: DialogInterface?) {
        (dialog as AlertDialog?)
            ?.getButton(DialogInterface.BUTTON_POSITIVE)
            ?.setOnClickListener {
                save()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "RedditSourceDialogFragment"

        const val REQUEST_KEY_SOURCE = "REQUEST_KEY_SOURCE"

        const val KEY_SOURCE = "KEY_SOURCE"
        const val KEY_INSTANCE = "KEY_INSTANCE"
        private const val KEY_INSTANCES = "KEY_INSTANCES"

        fun show(
            fragmentManager: FragmentManager,
            source: DataPreferences.RedditSource,
            instance: String?,
            instances: List<String>
        ) {
            RedditSourceDialogFragment().apply {
                arguments = bundleOf(
                    KEY_SOURCE to source,
                    KEY_INSTANCE to instance,
                    KEY_INSTANCES to instances
                )
            }.show(fragmentManager, TAG)
        }
    }
}
