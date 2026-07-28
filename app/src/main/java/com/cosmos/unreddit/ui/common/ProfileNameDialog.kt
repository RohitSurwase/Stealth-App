package com.cosmos.unreddit.ui.common

import android.content.DialogInterface
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.cosmos.unreddit.R
import com.cosmos.unreddit.databinding.DialogAddProfileBinding
import com.cosmos.unreddit.util.extension.text
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Name prompt used when creating or renaming a profile.
 *
 * Shared by the profile manager and the home profile panel so both enforce the same naming rules.
 */
object ProfileNameDialog {

    private const val PROFILE_NAME_MIN = 3
    private const val PROFILE_NAME_MAX = 20

    /**
     * [onValidName] is invoked only once [existingNames] and the length rules are satisfied; on a
     * validation error the dialog stays open and shows the error inline instead.
     */
    fun show(
        fragment: Fragment,
        @StringRes title: Int,
        @StringRes positiveButton: Int,
        existingNames: List<String>,
        initialName: String? = null,
        onValidName: (String) -> Unit
    ) {
        val profileBinding = DialogAddProfileBinding.inflate(
            fragment.requireActivity().layoutInflater
        ).apply {
            initialName?.let { inputName.editText?.setText(it) }
        }

        MaterialAlertDialogBuilder(fragment.requireContext())
            .setView(profileBinding.root)
            .setTitle(title)
            .setPositiveButton(positiveButton) { _, _ ->
                // Handled below so an invalid name does not dismiss the dialog
            }
            .setNeutralButton(R.string.dialog_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val name = profileBinding.inputName.text().toString()
                    val errorMessage = validate(fragment, name, existingNames)
                    if (errorMessage == null) {
                        onValidName(name)
                        dismiss()
                    } else {
                        profileBinding.inputName.error = errorMessage
                    }
                }
            }
    }

    private fun validate(
        fragment: Fragment,
        text: String,
        existingNames: List<String>
    ): String? {
        return when {
            text.length !in PROFILE_NAME_MIN..PROFILE_NAME_MAX -> {
                fragment.getString(R.string.profile_name_length_error)
            }
            existingNames.any { it.equals(text, true) } -> {
                fragment.getString(R.string.profile_already_exists_error)
            }
            text.isBlank() -> {
                fragment.getString(R.string.profile_blank_error)
            }
            else -> {
                null
            }
        }
    }
}
