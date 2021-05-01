package com.cosmos.unreddit.ui.common.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import androidx.appcompat.widget.AppCompatImageView

/**
 * Replacement for Switch in order to use vectors with gradients
 */
class CheckableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), Checkable {

    interface OnCheckedChangeListener {
        fun onCheckedChanged(checkableImageView: CheckableImageView, isChecked: Boolean)
    }

    private val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)

    private var checked: Boolean = false

    private var onCheckedChangeListener: OnCheckedChangeListener? = null

    init {
        setOnClickListener { toggle() }
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET)
        }
        return drawableState
    }

    override fun setChecked(checked: Boolean) {
        this.checked = checked
        onCheckedChangeListener?.onCheckedChanged(this, checked)
    }

    override fun isChecked(): Boolean {
        return checked
    }

    override fun toggle() {
        isChecked = !isChecked
    }

    fun setOnCheckedChangeListener(onCheckedChanged: (CheckableImageView, Boolean) -> Unit) {
        setOnCheckedChangeListener(
            object : OnCheckedChangeListener {
                override fun onCheckedChanged(
                    checkableImageView: CheckableImageView,
                    isChecked: Boolean
                ) {
                    onCheckedChanged.invoke(checkableImageView, isChecked)
                }
            }
        )
    }

    fun setOnCheckedChangeListener(onCheckedChangeListener: OnCheckedChangeListener) {
        this.onCheckedChangeListener = onCheckedChangeListener
    }
}
