package com.cosmos.unreddit.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.math.MathUtils
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.drakeet.drawer.FullDraggableContainer
import com.drakeet.drawer.FullDraggableHelper

class DrawerContent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MotionLayout(context, attrs, defStyleAttr), DrawerLayout.DrawerListener,
    FullDraggableHelper.Callback {

    private val helper = FullDraggableHelper(context, this)

    private lateinit var drawerLayout: DrawerLayout

    private fun ensureDrawerLayout() {
        drawerLayout = parent as? DrawerLayout
            ?: throw IllegalStateException("This $this must be added to a DrawerLayout")
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return helper.onTouchEvent(event)
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        return helper.onInterceptTouchEvent(event)
    }

    override fun onDrawerStateChanged(newState: Int) {
        // Ignore
    }

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
        progress = slideOffset
    }

    override fun onDrawerClosed(drawerView: View) {
        // Ignore
    }

    override fun onDrawerOpened(drawerView: View) {
        // Ignore
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ensureDrawerLayout()
        drawerLayout.addDrawerListener(this)
    }

    override fun getDrawerMainContainer(): View {
        return this
    }

    override fun isDrawerOpen(gravity: Int): Boolean {
        return drawerLayout.isDrawerOpen(gravity)
    }

    override fun hasEnabledDrawer(gravity: Int): Boolean {
        return drawerLayout.getDrawerLockMode(gravity) == DrawerLayout.LOCK_MODE_UNLOCKED &&
            findDrawerWithGravity(gravity) != null
    }

    override fun offsetDrawer(gravity: Int, offset: Float) {
        setDrawerToOffset(gravity, offset)
    }

    override fun smoothOpenDrawer(gravity: Int) {
        drawerLayout.openDrawer(gravity, true)
    }

    override fun smoothCloseDrawer(gravity: Int) {
        drawerLayout.closeDrawer(gravity, false)
    }

    override fun onDrawerDragging() {
        // Ignore
    }

    /**
     * From [FullDraggableContainer]
     */
    private fun setDrawerToOffset(gravity: Int, offset: Float) {
        val drawerView: View = findDrawerWithGravity(gravity) ?: return
        val slideOffsetPercent = MathUtils.clamp(offset / drawerView.width, 0f, 1f)
        try {
            val method = DrawerLayout::class.java.getDeclaredMethod(
                "moveDrawerToOffset",
                View::class.java,
                Float::class.javaPrimitiveType
            )
            method.isAccessible = true
            method.invoke(drawerLayout, drawerView, slideOffsetPercent)
            drawerView.visibility = VISIBLE
        } catch (e: Exception) {
            // throw to let developer know the api is changed
            throw RuntimeException(e)
        }
    }

    /**
     * From [FullDraggableContainer]
     */
    private fun findDrawerWithGravity(gravity: Int): View? {
        val absHorizGravity = GravityCompat.getAbsoluteGravity(
            gravity,
            ViewCompat.getLayoutDirection(drawerLayout)
        ) and Gravity.HORIZONTAL_GRAVITY_MASK
        val childCount = drawerLayout.childCount
        for (i in 0 until childCount) {
            val child = drawerLayout.getChildAt(i)
            val childAbsGravity = getDrawerViewAbsoluteGravity(child)
            if (childAbsGravity and Gravity.HORIZONTAL_GRAVITY_MASK == absHorizGravity) {
                return child
            }
        }
        return null
    }

    /**
     * From [FullDraggableContainer]
     */
    private fun getDrawerViewAbsoluteGravity(drawerView: View): Int {
        val gravity = (drawerView.layoutParams as DrawerLayout.LayoutParams).gravity
        return GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(drawerLayout))
    }
}
