package com.cosmos.unreddit.ui.common

import android.app.Dialog
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

open class FullscreenBottomSheetFragment : BottomSheetDialogFragment(), OnShowListener {

    private val bottomSheet: FrameLayout?
        get() {
            val bottomSheetDialog = dialog as BottomSheetDialog?

            return bottomSheetDialog
                ?.findViewById(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
        }

    protected val behavior: BottomSheetBehavior<FrameLayout>?
        get() = bottomSheet?.let { BottomSheetBehavior.from(it) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return createDialog().apply {
            setOnShowListener(this@FullscreenBottomSheetFragment)
        }
    }

    override fun onShow(dialog: DialogInterface?) {
        bottomSheet?.let { expandToFullscreen(it) }

        behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            maxWidth = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    private fun expandToFullscreen(bottomSheet: View) {
        val layoutParams = bottomSheet.layoutParams.apply {
            height = WindowManager.LayoutParams.MATCH_PARENT
            width = WindowManager.LayoutParams.MATCH_PARENT
        }
        bottomSheet.layoutParams = layoutParams
    }

    private fun createDialog(): Dialog {
        return object : BottomSheetDialog(requireContext(), theme) {
            override fun onAttachedToWindow() {
                super.onAttachedToWindow()

                findViewById<View>(com.google.android.material.R.id.container)
                    ?.fitsSystemWindows = false
                findViewById<View>(com.google.android.material.R.id.coordinator)
                    ?.fitsSystemWindows = false
            }
        }
    }
}