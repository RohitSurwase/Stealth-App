package com.cosmos.unreddit.ui.common.widget

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.cosmos.unreddit.R
import com.cosmos.unreddit.util.extension.fitToRange
import com.cosmos.unreddit.util.extension.sum
import com.google.android.material.imageview.ShapeableImageView

class AvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var avatar: ShapeableImageView
    private var initials: TextView

    private var text: String? = null

    private val backgroundColor: Int
        get() = text?.sum?.fitToRange(colorArray.indices)?.let { colorArray[it] } ?: colorArray[0]

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AvatarView,
            0, 0
        ).apply {
            try {
                text = getString(R.styleable.AvatarView_text)
            } finally {
                recycle()
            }
        }

        inflate(context, R.layout.view_avatar, this)

        avatar = findViewById(R.id.avatar)
        initials = findViewById(R.id.initials)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        updateView()
    }

    fun setText(text: String?) {
        this.text = text
        updateView()
    }

    private fun updateView() {
        initials.text = textToInitials()
        avatar.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, backgroundColor)
        )
    }

    private fun textToInitials(): String? {
        return text?.split(SPACE_REGEX)?.joinToString("") { it.first().uppercase() }
    }

    companion object {
        private val SPACE_REGEX = Regex("\\s")

        private val colorArray = arrayOf(
            R.color.profile_background_1,
            R.color.profile_background_2,
            R.color.profile_background_3,
            R.color.profile_background_4,
            R.color.profile_background_5,
            R.color.profile_background_6,
            R.color.profile_background_7,
            R.color.profile_background_8,
            R.color.profile_background_9,
            R.color.profile_background_10,
        )
    }
}
