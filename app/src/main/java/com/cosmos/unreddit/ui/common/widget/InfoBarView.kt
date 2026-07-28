package com.cosmos.unreddit.ui.common.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.cosmos.unreddit.R
import com.google.android.material.card.MaterialCardView

class InfoBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewStyle
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val transition: Slide by lazy {
        Slide().apply {
            duration = 500
            addTarget(this@InfoBarView)
        }
    }

    private var message: TextView
    private var action: Button

    private var textMessage: String? = null
    private var textAction: String? = null

    private var textMessageColor: Int = ContextCompat.getColor(context, R.color.text_color)
    private var textActionColor: Int = ContextCompat.getColor(context, R.color.text_color)

    @Slide.GravityFlag
    private var slideEdge: Int = Gravity.BOTTOM

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.InfoBarView,
            0, 0
        ).apply {
            try {
                if (hasValue(R.styleable.InfoBarView_textMessage)) {
                    textMessage = getString(R.styleable.InfoBarView_textMessage)
                }
                if (hasValue(R.styleable.InfoBarView_textAction)) {
                    textAction = getString(R.styleable.InfoBarView_textAction)
                }
                if (hasValue(R.styleable.InfoBarView_textMessageColor)) {
                    textMessageColor = getColor(
                        R.styleable.InfoBarView_textMessageColor,
                        ContextCompat.getColor(context, R.color.text_color)
                    )
                }
                if (hasValue(R.styleable.InfoBarView_textActionColor)) {
                    textActionColor = getColor(
                        R.styleable.InfoBarView_textActionColor,
                        ContextCompat.getColor(context, R.color.text_color)
                    )
                }
                if (hasValue(R.styleable.InfoBarView_slideEdge)) {
                    slideEdge = when (getInteger(R.styleable.InfoBarView_slideEdge, 0)) {
                        0 -> Gravity.START
                        1 -> Gravity.END
                        2 -> Gravity.TOP
                        3 -> Gravity.BOTTOM
                        else -> Gravity.BOTTOM
                    }
                }
            } finally {
                recycle()
            }
        }

        inflate(context, R.layout.view_info_bar, this)

        message = findViewById(R.id.text_message)
        action = findViewById(R.id.button_action)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        message.apply {
            text = textMessage
            setTextColor(textMessageColor)
        }

        action.apply {
            text = textAction
            setTextColor(textActionColor)
        }

        transition.slideEdge = slideEdge
    }

    fun setActionClickListener(action: () -> Unit) {
        this.action.setOnClickListener {
            action.invoke()
            hide()
        }
    }

    fun show() {
        getSceneRoot()?.let {
            TransitionManager.beginDelayedTransition(it, transition)
        }
        visibility = View.VISIBLE
    }

    fun hide() {
        getSceneRoot()?.let {
            TransitionManager.beginDelayedTransition(it, transition)
        }
        visibility = View.GONE
    }

    private fun getSceneRoot(): ViewGroup? {
        return parent as? ViewGroup
    }
}
