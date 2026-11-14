package com.cosmos.unreddit.ui.redditsource

import android.app.Dialog
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Filter
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
            radioReddit.isChecked = source == DataPreferences.RedditSource.REDDIT
            radioRedditScrap.isChecked = source == DataPreferences.RedditSource.REDDIT_SCRAP

            val isTeddit = source == DataPreferences.RedditSource.TEDDIT

            radioTeddit.isChecked = isTeddit
            listInstances.isVisible = isTeddit
            verifyInstance.isVisible = isTeddit
            refreshInstances.isVisible = isTeddit

            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                val isRedlib = checkedId == R.id.radio_teddit
                listInstances.isVisible = isRedlib
                verifyInstance.isVisible = isRedlib
                refreshInstances.isVisible = isRedlib
            }

            verifyInstance.setOnClickListener {
                val linkValidator = LinkValidator(textListInstances.text.toString())
                val host = linkValidator.validUrl?.host
                if (host.isNullOrEmpty()) {
                    listInstances.error = getString(R.string.instance_invalid_error)
                    return@setOnClickListener
                }
                listInstances.error = null
                startActivity(
                    RedlibVerificationActivity.intent(
                        requireContext(),
                        linkValidator.validUrl.toString()
                    )
                )
            }

            refreshInstances.setOnClickListener {
                doAndDismiss {
                    setFragmentResult(
                        REQUEST_KEY_SOURCE,
                        bundleOf(
                            KEY_REFRESH to true,
                            KEY_SOURCE to selectedSource(),
                            KEY_INSTANCE to textListInstances.text.toString()
                        )
                    )
                }
            }
        }
    }

    private fun selectedSource(): DataPreferences.RedditSource {
        return when (binding.radioGroup.checkedRadioButtonId) {
            R.id.radio_reddit -> DataPreferences.RedditSource.REDDIT
            R.id.radio_reddit_scrap -> DataPreferences.RedditSource.REDDIT_SCRAP
            R.id.radio_teddit -> DataPreferences.RedditSource.TEDDIT
            else -> DataPreferences.RedditSource.REDDIT
        }
    }

    private fun initSpinner() {
        // A plain ArrayAdapter filters the dropdown by the current text, which
        // would leave only the already selected instance visible. Keep the full
        // list every time the menu is opened instead.
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            instances
        ) {
            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        return FilterResults().apply {
                            values = instances
                            count = instances.size
                        }
                    }

                    @Suppress("UNCHECKED_CAST")
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        notifyDataSetChanged()
                    }
                }
            }
        }

        val savedInstance = instance.orEmpty().ifEmpty { adapter.getItem(0).orEmpty() }

        binding.textListInstances.run {
            setAdapter(adapter)
            setText(savedInstance, false)
        }
    }

    private fun save() {
        val source = selectedSource()
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
                    KEY_INSTANCE to linkValidator.validUrl?.toString()
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
        const val KEY_REFRESH = "KEY_REFRESH"
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
                    KEY_INSTANCES to ArrayList(instances)
                )
            }.show(fragmentManager, TAG)
        }
    }
}
