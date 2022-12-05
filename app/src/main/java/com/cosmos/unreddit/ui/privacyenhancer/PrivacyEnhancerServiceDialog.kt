package com.cosmos.unreddit.ui.privacyenhancer

import android.app.Dialog
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.ServiceExternal
import com.cosmos.unreddit.data.model.ServiceRedirect
import com.cosmos.unreddit.data.model.db.Redirect
import com.cosmos.unreddit.databinding.FragmentPrivacyEnhancerServiceBinding
import com.cosmos.unreddit.util.extension.doAndDismiss
import com.cosmos.unreddit.util.extension.parcelable
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PrivacyEnhancerServiceDialog : DialogFragment(), OnShowListener {

    private var _binding: FragmentPrivacyEnhancerServiceBinding? = null
    private val binding get() = _binding!!

    private lateinit var service: ServiceExternal
    private lateinit var redirect: Redirect
    private lateinit var instances: List<ServiceRedirect>

    private lateinit var adapter: PrivacyEnhancerInstanceAdapter

    private val mode: Redirect.RedirectMode
        get() = when {
            !binding.switchEnabled.isChecked -> Redirect.RedirectMode.OFF
            binding.checkboxAsk.isChecked -> Redirect.RedirectMode.ALWAYS_ASK
            else -> Redirect.RedirectMode.ON
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.run {
            service = parcelable(KEY_SERVICE) ?: throw IllegalStateException("Service is null")
            redirect = parcelable(KEY_REDIRECT) ?: Redirect(
                service.pattern,
                "",
                service.service,
                Redirect.RedirectMode.OFF
            )
            instances = service.redirect
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentPrivacyEnhancerServiceBinding.inflate(requireActivity().layoutInflater)

        initView()
        initSpinner()

        return MaterialAlertDialogBuilder(requireContext())
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
                setOnShowListener(this@PrivacyEnhancerServiceDialog)
            }
    }

    private fun initView() {
        binding.run {
            this.redirect = this@PrivacyEnhancerServiceDialog.redirect

            switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                listInstances.isEnabled = isChecked
                checkboxAsk.isEnabled = isChecked
            }
        }
    }

    private fun initSpinner() {
        // Map each instance with its service
        val map = instances
            .map { serviceRedirect ->
                serviceRedirect.instances.associateWith { serviceRedirect.name }
            }
            .flatMap { it.entries }
            .associate { it.key to it.value }

        adapter = PrivacyEnhancerInstanceAdapter(map)

        val savedInstance = redirect.redirect.ifEmpty { adapter.getItem(0) as String }

        binding.textListInstances.run {
            setAdapter(this@PrivacyEnhancerServiceDialog.adapter)
            setText(savedInstance, false)
        }
    }

    private fun save() {
        redirect.run {
            redirect = binding.textListInstances.text.toString()
            pattern = this@PrivacyEnhancerServiceDialog.service.pattern
            mode = this@PrivacyEnhancerServiceDialog.mode
        }
        doAndDismiss {
            setFragmentResult(
                REQUEST_KEY_REDIRECT,
                bundleOf(KEY_REDIRECT to redirect)
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
        private const val TAG = "PrivacyEnhancerServiceDialog"

        const val REQUEST_KEY_REDIRECT = "REQUEST_KEY_REDIRECT"

        private const val KEY_SERVICE = "KEY_SERVICE"
        const val KEY_REDIRECT = "KEY_REDIRECT"

        fun show(
            fragmentManager: FragmentManager,
            service: ServiceExternal,
            redirect: Redirect?
        ) {
            PrivacyEnhancerServiceDialog().apply {
                arguments = bundleOf(
                    KEY_SERVICE to service,
                    KEY_REDIRECT to redirect
                )
            }.show(fragmentManager, TAG)
        }
    }
}
