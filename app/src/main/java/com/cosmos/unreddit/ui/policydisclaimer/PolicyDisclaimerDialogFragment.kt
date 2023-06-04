package com.cosmos.unreddit.ui.policydisclaimer

import android.app.Dialog
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.cosmos.unreddit.R
import com.cosmos.unreddit.databinding.FragmentPolicyDisclaimerBinding
import com.cosmos.unreddit.util.ClickableMovementMethod
import com.cosmos.unreddit.util.LinkHandler
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PolicyDisclaimerDialogFragment : DialogFragment(), ClickableMovementMethod.OnClickListener,
    OnShowListener {

    private var _binding: FragmentPolicyDisclaimerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PolicyDisclaimerViewModel by viewModels()

    @Inject
    lateinit var linkHandler: LinkHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentPolicyDisclaimerBinding.inflate(requireActivity().layoutInflater)

        binding.message.movementMethod = ClickableMovementMethod(this)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_policy_disclaimer_title)
            .setView(binding.root)
            .setPositiveButton(R.string.ok) { _, _ ->
                // Ignore
            }
            .setCancelable(false)
            .create()
            .apply {
                setOnShowListener(this@PolicyDisclaimerDialogFragment)
            }
    }

    override fun onLinkClick(link: String) {
        linkHandler.openBrowser(link)
    }

    override fun onLinkLongClick(link: String) {
        Toast.makeText(requireContext(), link, Toast.LENGTH_SHORT).show()
    }

    override fun onClick() {
        // Ignore
    }

    override fun onLongClick() {
        // Ignore
    }

    override fun onShow(dialog: DialogInterface?) {
        viewModel.setPolicyDisclaimerShown(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "PolicyDisclaimerDialogFragment"

        fun show(fragmentManager: FragmentManager) {
            PolicyDisclaimerDialogFragment().show(fragmentManager, TAG)
        }
    }
}
