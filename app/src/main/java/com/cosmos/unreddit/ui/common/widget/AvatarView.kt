package com.cosmos.unreddit.ui.common.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.cosmos.unreddit.R
import com.google.android.material.imageview.ShapeableImageView
import java.util.Locale

class AvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var avatar: ShapeableImageView
    private var initials: TextView

    private var text: String? = null

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
        initials.text = textToInitials()
    }

    fun setText(text: String?) {
        this.text = text
        initials.text = textToInitials()
    }

    private fun textToInitials(): String? {
        return text?.toUpperCase(Locale.getDefault())?.split("\\s")?.map { it.first() }
            ?.joinToString("")
    }
}
