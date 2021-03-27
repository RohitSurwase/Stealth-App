package com.cosmos.unreddit.ui.common

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

open class FullscreenBottomSheetFragment : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.setOnShowListener {
            val bottomSheetDialog = it as BottomSheetDialog

            val bottomSheet = bottomSheetDialog
                .findViewById(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(bottomSheet)
                expandToFullscreen(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        return dialog
    }

    private fun expandToFullscreen(bottomSheet: View) {
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        bottomSheet.layoutParams = layoutParams
    }
}