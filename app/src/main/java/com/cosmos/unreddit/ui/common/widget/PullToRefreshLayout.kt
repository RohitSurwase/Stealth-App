package com.cosmos.unreddit.ui.common.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.Transformation
import androidx.core.view.MotionEventCompat
import androidx.core.view.NestedScrollingChild
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.NestedScrollingParent
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import kotlin.math.abs
import kotlin.math.pow

/**
 * Adapted from [SSPullToRefresh](https://github.com/SimformSolutionsPvtLtd/SSPullToRefresh)
 */
class PullToRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), NestedScrollingParent, NestedScrollingChild {

    interface RefreshCallback {
        /**
         * When the content view has reached to the start point and refresh has been completed, view
         * will be reset.
         */
        fun reset()

        /**
         * Refresh View is refreshing
         */
        fun refreshing()

        /**
         * refresh has been completed
         */
        fun refreshComplete()

        /**
         * Refresh View is dropped down to the refresh point
         */
        fun pullToRefresh()

        /**
         * Refresh View is released into the refresh point
         */
        fun releaseToRefresh()

        /**
         * @param pullDistance The drop-down distance of the refresh View
         * @param pullProgress The drop-down progress of the refresh View and the pullProgress may
         * be more than 1.0f
         *
         * pullProgress = pullDistance / refreshTargetOffset
         */
        fun pullProgress(pullDistance: Float, pullProgress: Float)
    }

    private val logTag = this.javaClass.name

    // NestedScroll
    private var mTotalUnconsumed = 0f
    private var mNestedScrollInProgress = false
    private val mParentScrollConsumed = IntArray(2)
    private val mParentOffsetInWindow = IntArray(2)
    private val mNestedScrollingChildHelper: NestedScrollingChildHelper
    private val mNestedScrollingParentHelper: NestedScrollingParentHelper

    //whether to remind the callback listener(OnRefreshListener)
    private var mRefreshInitialOffset: Float = 0.0f
    private var mRefreshTargetOffset: Float = 0f
    private var mInitialDownY = 0f
    private var mInitialScrollY = 0f
    private var mInitialMotionY = 0f
    private var mCurrentTouchOffsetY = 0f
    private var mTargetOrRefreshViewOffsetY: Float = 0.0f
    private var mFrom = 0
    private var mIsAnimatingToStart = false
    private var mIsFitRefresh = false
    private var mIsBeingDragged = false
    private var mNotifyListener = false
    private var mDispatchTargetTouchDown = false
    private var mRefreshViewIndex = INVALID_INDEX
    private var mActivePointerId = INVALID_POINTER
    private var mAnimateToStartDuration = DEFAULT_ANIMATE_DURATION
    private var mAnimateToRefreshDuration = DEFAULT_ANIMATE_DURATION

    // Whether the client has set a custom refreshing position
    private var mUsingCustomRefreshTargetOffset = false

    // Whether the client has set a custom starting position
    private var mUsingCustomRefreshInitialOffset = false

    // Whether or not the RefreshView has been measured.
    private var mRefreshViewMeasured = false
    private var mRefreshStyle: RefreshStyle = RefreshStyle.NORMAL
    private var mTarget: View? = null
    private var mDragDistanceConverter: DragDistanceConverter
    private var mRefreshLayoutParams: ViewGroup.LayoutParams

    var refreshView: View
        private set

    var isRefreshing = false
        private set

    init {
        val metrics = resources.displayMetrics
        mRefreshLayoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        mRefreshTargetOffset = DEFAULT_REFRESH_TARGET_OFFSET_DP * metrics.density
        mNestedScrollingParentHelper = NestedScrollingParentHelper(this)
        mNestedScrollingChildHelper = NestedScrollingChildHelper(this)
        isNestedScrollingEnabled = true
        isChildrenDrawingOrderEnabled = true

        // init mRefreshView
        refreshView = PullToRefreshView(context)
        refreshView.visibility = GONE
        addView(refreshView, mRefreshLayoutParams)

        // init mDragDistanceConverter
        mDragDistanceConverter = DragDistanceConverter()
    }

    private var mOnRefreshListener: OnRefreshListener? = null
    private var mAnimateToStartInterpolator: Interpolator = DecelerateInterpolator(
        DECELERATE_INTERPOLATION_FACTOR
    )
    private var mAnimateToRefreshInterpolator: Interpolator = DecelerateInterpolator(
        DECELERATE_INTERPOLATION_FACTOR
    )
    private val mAnimateToRefreshingAnimation: Animation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            when (mRefreshStyle) {
                RefreshStyle.FLOAT -> {
                    val refreshTargetOffset = mRefreshTargetOffset + mRefreshInitialOffset
                    animateToTargetOffset(
                        refreshTargetOffset,
                        refreshView.top.toFloat(),
                        interpolatedTime
                    )
                }
                else -> {
                    mTarget?.let {
                        animateToTargetOffset(
                            mRefreshTargetOffset,
                            it.top.toFloat(),
                            interpolatedTime
                        )
                    }
                }
            }
        }
    }

    private val mAnimateToStartAnimation: Animation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            when (mRefreshStyle) {
                RefreshStyle.FLOAT -> animateToTargetOffset(
                    mRefreshInitialOffset,
                    refreshView.top.toFloat(),
                    interpolatedTime
                )
                else -> {
                    mTarget?.let {
                        animateToTargetOffset(0.0f, it.top.toFloat(), interpolatedTime)
                    }
                }
            }
        }
    }

    private fun animateToTargetOffset(
        targetEnd: Float,
        currentOffset: Float,
        interpolatedTime: Float
    ) {
        val targetOffset = (mFrom + (targetEnd - mFrom) * interpolatedTime).toInt()
        setTargetOrRefreshViewOffsetY((targetOffset - currentOffset).toInt())
    }

    private val mRefreshingListener: Animation.AnimationListener =
        object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                mIsAnimatingToStart = true
                (refreshView as? RefreshCallback)?.refreshing()
            }

            override fun onAnimationRepeat(animation: Animation) {
                // Ignore
            }

            override fun onAnimationEnd(animation: Animation) {
                if (mNotifyListener) {
                    mOnRefreshListener?.onRefresh()
                }
                mIsAnimatingToStart = false
            }
        }

    private val mResetListener: Animation.AnimationListener = object : Animation.AnimationListener {

        override fun onAnimationStart(animation: Animation) {
            mIsAnimatingToStart = true
            (refreshView as? RefreshCallback)?.refreshComplete()
        }

        override fun onAnimationRepeat(animation: Animation) {
            // Ignore
        }

        override fun onAnimationEnd(animation: Animation) {
            reset()
        }
    }

    override fun onDetachedFromWindow() {
        reset()
        clearAnimation()
        super.onDetachedFromWindow()
    }

    private fun reset() {
        setTargetOrRefreshViewToInitial()
        mCurrentTouchOffsetY = 0.0f
        (refreshView as? RefreshCallback)?.reset()
        refreshView.visibility = GONE
        isRefreshing = false
        mIsAnimatingToStart = false
    }

    private fun setTargetOrRefreshViewToInitial() {
        when (mRefreshStyle) {
            RefreshStyle.FLOAT -> {
                setTargetOrRefreshViewOffsetY(
                    (mRefreshInitialOffset - mTargetOrRefreshViewOffsetY).toInt()
                )
            }
            else -> setTargetOrRefreshViewOffsetY((0 - mTargetOrRefreshViewOffsetY).toInt())
        }
    }

    /**
     * @param refreshView  must implements the interface IRefreshStatus
     */
    @Suppress("Unused")
    fun setRefreshView(refreshView: View) {
        if (this.refreshView === refreshView) {
            return
        }
        if (this.refreshView.parent != null) {
            (this.refreshView.parent as ViewGroup).removeView(this.refreshView)
        }
        refreshView.visibility = GONE
        addView(refreshView, mRefreshLayoutParams)
        this.refreshView = refreshView
    }

    @Suppress("Unused")
    fun setRefreshViewParams(params: ViewGroup.LayoutParams) {
        mRefreshLayoutParams = params
        refreshView.layoutParams = MarginLayoutParams(params.width, params.height)
    }

    @Suppress("Unused")
    fun setDragDistanceConverter(dragDistanceConverter: DragDistanceConverter) {
        mDragDistanceConverter = dragDistanceConverter
    }

    /**
     * @param refreshTargetOffset The minimum distance that trigger refresh.
     */
    @Suppress("Unused")
    fun setRefreshTargetOffset(refreshTargetOffset: Float) {
        mRefreshTargetOffset = refreshTargetOffset
        mUsingCustomRefreshTargetOffset = true
        requestLayout()
    }

    /**
     * @param refreshInitialOffset the top position of the [.mRefreshView] relative to its parent.
     */
    @Suppress("Unused")
    fun setRefreshInitialOffset(refreshInitialOffset: Float) {
        mRefreshInitialOffset = refreshInitialOffset
        mUsingCustomRefreshInitialOffset = true
        requestLayout()
    }

    override fun getChildDrawingOrder(childCount: Int, i: Int): Int {
        when (mRefreshStyle) {
            RefreshStyle.FLOAT -> return when {
                mRefreshViewIndex < 0 -> {
                    i
                }
                i == childCount-1 -> {
                    // Draw the selected child last
                    mRefreshViewIndex
                }
                i >= mRefreshViewIndex -> {
                    // Move the children after the selected child earlier one
                    i + 1
                }
                else -> {
                    // Keep the children before the selected child the same
                    i
                }
            }
            else -> return when {
                mRefreshViewIndex < 0 -> {
                    i
                }
                i == 0 -> {
                    // Draw the selected child first
                    mRefreshViewIndex
                }
                i <= mRefreshViewIndex -> {
                    // Move the children before the selected child earlier one
                    i - 1
                }
                else -> {
                    i
                }
            }
        }
    }

    override fun requestDisallowInterceptTouchEvent(b: Boolean) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if (mTarget != null && !ViewCompat.isNestedScrollingEnabled(mTarget!!)) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(b)
        }
    }

    // NestedScrollingParent
    override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int): Boolean {
        return when (mRefreshStyle) {
            RefreshStyle.FLOAT -> (isEnabled
                    && canChildScrollUp(mTarget)
                    && !isRefreshing
                    && nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL != 0)
            else -> (isEnabled
                    && canChildScrollUp(mTarget)
                    && nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL != 0)
        }
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes)
        // Dispatch up to the nested parent
        startNestedScroll(axes and ViewCompat.SCROLL_AXIS_VERTICAL)
        mTotalUnconsumed = 0f
        mNestedScrollInProgress = true
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
        if (dy > 0 && mTotalUnconsumed > 0) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = dy - mTotalUnconsumed.toInt()
                mTotalUnconsumed = 0f
            } else {
                mTotalUnconsumed -= dy.toFloat()
                consumed[1] = dy
            }
            moveSpinner(mTotalUnconsumed)
        }

        // Now let our nested parent consume the leftovers
        val parentConsumed = mParentScrollConsumed
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0]
            consumed[1] += parentConsumed[1]
        }
    }

    override fun getNestedScrollAxes(): Int {
        return mNestedScrollingParentHelper.nestedScrollAxes
    }

    override fun onStopNestedScroll(target: View) {
        mNestedScrollingParentHelper.onStopNestedScroll(target)
        mNestedScrollInProgress = false
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (mTotalUnconsumed > 0) {
            finishSpinner()
            mTotalUnconsumed = 0f
        }
        // Dispatch up our nested parent
        stopNestedScroll()
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int
    ) {
        // Dispatch up to the nested parent first
        dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            mParentOffsetInWindow
        )

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        val dy = dyUnconsumed + mParentOffsetInWindow[1]
        if (dy < 0) {
            mTotalUnconsumed += abs(dy).toFloat()
            moveSpinner(mTotalUnconsumed)
        }
    }

    // NestedScrollingChild
    override fun setNestedScrollingEnabled(enabled: Boolean) {
        mNestedScrollingChildHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return mNestedScrollingChildHelper.startNestedScroll(axes)
    }

    override fun stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll()
    }

    override fun hasNestedScrollingParent(): Boolean {
        return mNestedScrollingChildHelper.hasNestedScrollingParent()
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?
    ): Boolean {
        return mNestedScrollingChildHelper.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            offsetInWindow
        )
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ): Boolean {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        return dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun onNestedFling(
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {

        if (childCount == 0) {
            return
        }
        ensureTarget()
        if (mTarget == null) {
            return
        }
        val width = measuredWidth
        val height = measuredHeight
        val targetTop = reviseTargetLayoutTop(paddingTop)
        val targetLeft = paddingLeft
        val targetRight = targetLeft + width - paddingLeft - paddingRight
        val targetBottom = targetTop + height - paddingTop - paddingBottom
        try {
            mTarget!!.layout(targetLeft, targetTop, targetRight, targetBottom)
        } catch (ignored: Exception) {
            Log.e(logTag, "error: ignored=" + ignored.toString() + " " + ignored.stackTrace.toString())
        }
        val refreshViewLeft = (width - refreshView.measuredWidth) / 2
        val refreshViewTop = reviseRefreshViewLayoutTop(mRefreshInitialOffset.toInt())
        val refreshViewRight = (width + refreshView.measuredWidth) / 2
        val refreshViewBottom = refreshViewTop + refreshView.measuredHeight
        refreshView.layout(refreshViewLeft, refreshViewTop, refreshViewRight, refreshViewBottom)
    }

    private fun reviseTargetLayoutTop(layoutTop: Int): Int {
        return when (mRefreshStyle) {
            RefreshStyle.FLOAT -> layoutTop
            RefreshStyle.PINNED -> layoutTop + mTargetOrRefreshViewOffsetY.toInt()
            else -> layoutTop + mTargetOrRefreshViewOffsetY.toInt()
        }
    }

    private fun reviseRefreshViewLayoutTop(layoutTop: Int): Int {
        return when (mRefreshStyle) {
            RefreshStyle.FLOAT -> layoutTop + mTargetOrRefreshViewOffsetY.toInt()
            RefreshStyle.PINNED -> layoutTop
            else -> layoutTop + mTargetOrRefreshViewOffsetY.toInt()
        }
    }

    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        ensureTarget()
        if (mTarget == null) {
            return
        }
        measureTarget()
        measureRefreshView(widthMeasureSpec, heightMeasureSpec)
        if (!mRefreshViewMeasured && !mUsingCustomRefreshInitialOffset) {
            when (mRefreshStyle) {
                RefreshStyle.PINNED -> {
                    mRefreshInitialOffset = 0.0f
                    mTargetOrRefreshViewOffsetY = mRefreshInitialOffset
                }
                RefreshStyle.FLOAT -> {
                    mRefreshInitialOffset = -refreshView.measuredHeight.toFloat()
                    mTargetOrRefreshViewOffsetY = mRefreshInitialOffset
                }
                else -> {
                    mTargetOrRefreshViewOffsetY = 0.0f
                    mRefreshInitialOffset = -refreshView.measuredHeight.toFloat()
                }
            }
        }
        if (!mRefreshViewMeasured && !mUsingCustomRefreshTargetOffset) {
            if (mRefreshTargetOffset < refreshView.measuredHeight) {
                mRefreshTargetOffset = refreshView.measuredHeight.toFloat()
            }
        }
        mRefreshViewMeasured = true
        mRefreshViewIndex = -1
        for (index in 0 until childCount) {
            if (getChildAt(index) === refreshView) {
                mRefreshViewIndex = index
                break
            }
        }
    }

    private fun measureTarget() {
        mTarget?.measure(
            MeasureSpec.makeMeasureSpec(
                measuredWidth - paddingLeft - paddingRight,
                MeasureSpec.EXACTLY
            ),
            MeasureSpec.makeMeasureSpec(
                measuredHeight - paddingTop - paddingBottom,
                MeasureSpec.EXACTLY
            )
        )
    }

    private fun measureRefreshView(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val lp = refreshView.layoutParams as MarginLayoutParams
        val childWidthMeasureSpec: Int = if (lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
            val width = 0.coerceAtLeast(
                measuredWidth - paddingLeft - paddingRight - lp.leftMargin - lp.rightMargin
            )
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        } else {
            getChildMeasureSpec(
                widthMeasureSpec,
                paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin, lp.width
            )
        }
        val childHeightMeasureSpec: Int = if (lp.height == ViewGroup.LayoutParams.MATCH_PARENT) {
            val height = 0.coerceAtLeast(
                measuredHeight - paddingTop - paddingBottom - lp.topMargin - lp.bottomMargin
            )
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        } else {
            getChildMeasureSpec(
                heightMeasureSpec,
                paddingTop + paddingBottom + lp.topMargin + lp.bottomMargin, lp.height
            )
        }
        refreshView.measure(childWidthMeasureSpec, childHeightMeasureSpec)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onStopNestedScroll(this)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        ensureTarget()
        if (mTarget == null) {
            return false
        }
        when (mRefreshStyle) {
            RefreshStyle.FLOAT -> {
                if (!isEnabled || canChildScrollUp(mTarget) || isRefreshing ||
                    mNestedScrollInProgress) {
                    // Fail fast if we're not in a state where a swipe is possible
                    return false
                }
            }
            else -> if ((!isEnabled || (canChildScrollUp(mTarget) && !mDispatchTargetTouchDown))) {
                return false
            }
        }
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mActivePointerId = ev.getPointerId(0)
                mIsBeingDragged = false
                val initialDownY = getMotionEventY(ev, mActivePointerId)
                if (initialDownY == -1f) {
                    return false
                }

                // Animation.AnimationListener.onAnimationEnd() can't be ensured to be called
                if (mAnimateToRefreshingAnimation.hasEnded() &&
                    mAnimateToStartAnimation.hasEnded()) {
                    mIsAnimatingToStart = false
                }
                mInitialDownY = initialDownY
                mInitialScrollY = mTargetOrRefreshViewOffsetY
                mDispatchTargetTouchDown = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (mActivePointerId == INVALID_POINTER) {
                    return false
                }
                val activeMoveY = getMotionEventY(ev, mActivePointerId)
                if (activeMoveY == -1f) {
                    return false
                }
                initDragStatus(activeMoveY)
            }
            MotionEvent.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mIsBeingDragged = false
                mActivePointerId = INVALID_POINTER
            }
            else -> {
            }
        }
        return mIsBeingDragged
    }

    @SuppressLint("ClickableViewAccessibility")
    @Suppress("Deprecation")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        ensureTarget()
        if (mTarget == null) {
            return false
        }
        when (mRefreshStyle) {
            RefreshStyle.FLOAT -> {
                if (!isEnabled || canChildScrollUp(mTarget) || mNestedScrollInProgress) {
                    // Fail fast if we're not in a state where a swipe is possible
                    return false
                }
            }
            else -> if ((!isEnabled || (canChildScrollUp(mTarget) && !mDispatchTargetTouchDown))) {
                return false
            }
        }
        if (mRefreshStyle == RefreshStyle.FLOAT && (canChildScrollUp(mTarget) ||
                    mNestedScrollInProgress)) {
            return false
        }
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                mActivePointerId = ev.getPointerId(0)
                mIsBeingDragged = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (mActivePointerId == INVALID_POINTER) {
                    return false
                }
                val activeMoveY = getMotionEventY(ev, mActivePointerId)
                if (activeMoveY == -1f) {
                    return false
                }
                val overScrollY: Float
                if (mIsAnimatingToStart) {
                    overScrollY = targetOrRefreshViewTop.toFloat()
                    mInitialMotionY = activeMoveY
                    mInitialScrollY = overScrollY
                } else {
                    overScrollY = activeMoveY - mInitialMotionY + mInitialScrollY
                }
                if (isRefreshing) {
                    //note: float style will not come here
                    if (overScrollY <= 0) {
                        if (mDispatchTargetTouchDown) {
                            mTarget!!.dispatchTouchEvent(ev)
                        } else {
                            val obtain = MotionEvent.obtain(ev)
                            obtain.action = MotionEvent.ACTION_DOWN
                            mDispatchTargetTouchDown = true
                            mTarget!!.dispatchTouchEvent(obtain)
                        }
                    } else if (overScrollY > 0 && overScrollY < mRefreshTargetOffset) {
                        if (mDispatchTargetTouchDown) {
                            val obtain = MotionEvent.obtain(ev)
                            obtain.action = MotionEvent.ACTION_CANCEL
                            mDispatchTargetTouchDown = false
                            mTarget!!.dispatchTouchEvent(obtain)
                        }
                    }
                    moveSpinner(overScrollY)
                } else {
                    if (mIsBeingDragged) {
                        if (overScrollY > 0) {
                            moveSpinner(overScrollY)
                        } else {
                            return false
                        }
                    } else {
                        initDragStatus(activeMoveY)
                    }
                }
            }
            MotionEventCompat.ACTION_POINTER_DOWN -> { onNewerPointerDown(ev) }
            MotionEvent.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (mActivePointerId == INVALID_POINTER ||
                    getMotionEventY(ev, mActivePointerId) == -1f) {
                    resetTouchEvent()
                    return false
                }
                if (isRefreshing || mIsAnimatingToStart) {
                    if (mDispatchTargetTouchDown) {
                        mTarget!!.dispatchTouchEvent(ev)
                    }
                    resetTouchEvent()
                    return false
                }
                resetTouchEvent()
                finishSpinner()
                return false
            }
            else -> {
            }
        }
        return true
    }

    private fun resetTouchEvent() {
        mInitialScrollY = 0.0f
        mIsBeingDragged = false
        mDispatchTargetTouchDown = false
        mActivePointerId = INVALID_POINTER
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    fun setRefreshing(refreshing: Boolean) {
        if (refreshing && !isRefreshing) {
            isRefreshing = true
            mNotifyListener = false
            animateToRefreshingPosition(mTargetOrRefreshViewOffsetY.toInt(), mRefreshingListener)
        } else {
            setRefreshing(refreshing, false)
        }
    }

    private fun setRefreshing(refreshing: Boolean, notify: Boolean) {
        if (isRefreshing != refreshing) {
            mNotifyListener = notify
            isRefreshing = refreshing
            if (refreshing) {
                animateToRefreshingPosition(
                    mTargetOrRefreshViewOffsetY.toInt(),
                    mRefreshingListener
                )
            } else {
                animateOffsetToStartPosition(mTargetOrRefreshViewOffsetY.toInt(), mResetListener)
            }
        }
    }

    private fun initDragStatus(activeMoveY: Float) {
        val diff = activeMoveY - mInitialDownY
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        if (isRefreshing && (diff > touchSlop || mTargetOrRefreshViewOffsetY > 0)) {
            mIsBeingDragged = true
            mInitialMotionY = mInitialDownY + touchSlop
            //scroll direction: from up to down
        } else if (!mIsBeingDragged && diff > touchSlop) {
            mInitialMotionY = mInitialDownY + touchSlop
            mIsBeingDragged = true
        }
    }

    private fun animateOffsetToStartPosition(from: Int, listener: Animation.AnimationListener?) {
        clearAnimation()
        if (computeAnimateToStartDuration(from.toFloat()) <= 0) {
            mAnimateToStartAnimation.cancel()
            return
        }
        mFrom = from
        mAnimateToStartAnimation.reset()
        mAnimateToStartAnimation.duration = computeAnimateToStartDuration(from.toFloat()).toLong()
        mAnimateToStartAnimation.interpolator = mAnimateToStartInterpolator
        if (listener != null) {
            mAnimateToStartAnimation.setAnimationListener(listener)
        }
        startAnimation(mAnimateToStartAnimation)
    }

    private fun animateToRefreshingPosition(from: Int, listener: Animation.AnimationListener?) {
        clearAnimation()
        if (computeAnimateToRefreshingDuration(from.toFloat()) <= 0) {
            mAnimateToRefreshingAnimation.cancel()
            return
        }
        mFrom = from
        mAnimateToRefreshingAnimation.reset()
        mAnimateToRefreshingAnimation.duration =
            computeAnimateToRefreshingDuration(from.toFloat()).toLong()
        mAnimateToRefreshingAnimation.interpolator = mAnimateToRefreshInterpolator
        if (listener != null) {
            mAnimateToRefreshingAnimation.setAnimationListener(listener)
        }
        startAnimation(mAnimateToRefreshingAnimation)
    }

    private fun computeAnimateToRefreshingDuration(from: Float): Int {
        if (from < mRefreshInitialOffset) {
            return 0
        }
        return when (mRefreshStyle) {
            RefreshStyle.FLOAT -> {
                (0.0f.coerceAtLeast(
                    1.0f.coerceAtMost(abs(
                        from - mRefreshInitialOffset - mRefreshTargetOffset
                    ) / mRefreshTargetOffset)
                ) * mAnimateToRefreshDuration).toInt()
            }
            else -> {
                (0.0f.coerceAtLeast(
                    1.0f.coerceAtMost(abs(
                        from - mRefreshTargetOffset
                    ) / mRefreshTargetOffset)
                ) * mAnimateToRefreshDuration).toInt()
            }
        }
    }

    private fun computeAnimateToStartDuration(from: Float): Int {
        if (from < mRefreshInitialOffset) {
            return 0
        }
        return when (mRefreshStyle) {
            RefreshStyle.FLOAT -> {
                (0.0f.coerceAtLeast(
                    1.0f.coerceAtMost(abs(
                        from - mRefreshInitialOffset
                    ) / mRefreshTargetOffset)
                ) * mAnimateToStartDuration).toInt()
            }
            else -> {
                (0.0f.coerceAtLeast(
                    1.0f.coerceAtMost(
                        abs(from) / mRefreshTargetOffset
                    )
                ) * mAnimateToStartDuration).toInt()
            }
        }
    }

    /**
     * @param targetOrRefreshViewOffsetY the top position of the target
     * or the RefreshView relative to its parent.
     */
    private fun moveSpinner(targetOrRefreshViewOffsetY: Float) {
        mCurrentTouchOffsetY = targetOrRefreshViewOffsetY
        var convertScrollOffset: Float
        val refreshTargetOffset: Float
        if (!isRefreshing) {
            when (mRefreshStyle) {
                RefreshStyle.FLOAT -> {
                    convertScrollOffset = mRefreshInitialOffset +
                            mDragDistanceConverter.convert(
                                targetOrRefreshViewOffsetY,
                                mRefreshTargetOffset
                            )
                    refreshTargetOffset = mRefreshTargetOffset
                }
                else -> {
                    convertScrollOffset = mDragDistanceConverter.convert(
                        targetOrRefreshViewOffsetY,
                        mRefreshTargetOffset
                    )
                    refreshTargetOffset = mRefreshTargetOffset
                }
            }
        } else {
            //The Float style will never come here
            convertScrollOffset = if (targetOrRefreshViewOffsetY > mRefreshTargetOffset) {
                mRefreshTargetOffset
            } else {
                targetOrRefreshViewOffsetY
            }
            if (convertScrollOffset < 0.0f) {
                convertScrollOffset = 0.0f
            }
            refreshTargetOffset = mRefreshTargetOffset
        }
        if (!isRefreshing) {
            if (convertScrollOffset > refreshTargetOffset && !mIsFitRefresh) {
                mIsFitRefresh = true
                (refreshView as? RefreshCallback)?.pullToRefresh()
            } else if (convertScrollOffset <= refreshTargetOffset && mIsFitRefresh) {
                mIsFitRefresh = false
                (refreshView as? RefreshCallback)?.releaseToRefresh()
            }
        }
        setTargetOrRefreshViewOffsetY((convertScrollOffset - mTargetOrRefreshViewOffsetY).toInt())
    }

    private fun finishSpinner() {
        if (isRefreshing || mIsAnimatingToStart) {
            return
        }
        val scrollY = targetOrRefreshViewOffset.toFloat()
        if (scrollY > mRefreshTargetOffset) {
            setRefreshing(refreshing = true, notify = true)
        } else {
            isRefreshing = false
            animateOffsetToStartPosition(mTargetOrRefreshViewOffsetY.toInt(), mResetListener)
        }
    }

    private fun onNewerPointerDown(ev: MotionEvent) {
        val index: Int = ev.actionIndex
        mActivePointerId = ev.getPointerId(index)
        mInitialMotionY = getMotionEventY(ev, mActivePointerId) - mCurrentTouchOffsetY
    }

    @Suppress("Deprecation")
    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex: Int = MotionEventCompat.getActionIndex(ev)
        val pointerId: Int = MotionEventCompat.getPointerId(ev, pointerIndex)
        if (pointerId == mActivePointerId) {
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            mActivePointerId = ev.getPointerId(newPointerIndex)
        }
        mInitialMotionY = getMotionEventY(ev, mActivePointerId) - mCurrentTouchOffsetY
    }

    private fun setTargetOrRefreshViewOffsetY(offsetY: Int) {
        if (mTarget == null) {
            return
        }
        mTargetOrRefreshViewOffsetY = when (mRefreshStyle) {
            RefreshStyle.FLOAT -> {
                refreshView.offsetTopAndBottom(offsetY)
                refreshView.top.toFloat()
            }
            RefreshStyle.PINNED -> {
                mTarget!!.offsetTopAndBottom(offsetY)
                mTarget!!.top.toFloat()
            }
            else -> {
                mTarget!!.offsetTopAndBottom(offsetY)
                refreshView.offsetTopAndBottom(offsetY)
                mTarget!!.top.toFloat()
            }
        }
        when (mRefreshStyle) {
            RefreshStyle.FLOAT -> (refreshView as? RefreshCallback)?.pullProgress(
                mTargetOrRefreshViewOffsetY,
                (mTargetOrRefreshViewOffsetY - mRefreshInitialOffset) / mRefreshTargetOffset
            )
            else -> (refreshView as? RefreshCallback)?.pullProgress(
                mTargetOrRefreshViewOffsetY,
                mTargetOrRefreshViewOffsetY / mRefreshTargetOffset
            )
        }
        if (mCurrentTouchOffsetY != 0f && refreshView.visibility != VISIBLE) {
            refreshView.visibility = VISIBLE
        }
        invalidate()
    }

    private val targetOrRefreshViewTop: Int
        get() {
            return when (mRefreshStyle) {
                RefreshStyle.FLOAT -> refreshView.top
                else -> mTarget!!.top
            }
        }
    private val targetOrRefreshViewOffset: Int
        get() {
            return when (mRefreshStyle) {
                RefreshStyle.FLOAT -> (refreshView.top - mRefreshInitialOffset).toInt()
                else -> mTarget!!.top
            }
        }

    private fun getMotionEventY(ev: MotionEvent, activePointerId: Int): Float {
        val index: Int = ev.findPointerIndex(activePointerId)
        return if (index < 0) {
            (-1).toFloat()
        } else ev.getY(index)
    }

    @Suppress("Deprecation")
    private fun canChildScrollUp(mTarget: View?): Boolean {
        if (mTarget == null) {
            return false
        }
        if (mTarget is ViewGroup) {
            val childCount = mTarget.childCount
            for (i in 0 until childCount) {
                val child = mTarget.getChildAt(i)
                if (canChildScrollUp(child)) {
                    return true
                }
            }
        }
        return ViewCompat.canScrollVertically(mTarget, -1)
    }

    private fun ensureTarget() {
        if (!isTargetValid) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child != refreshView) {
                    mTarget = child
                    break
                }
            }
        }
    }

    private val isTargetValid: Boolean
        get() {
            for (i in 0 until childCount) {
                if (mTarget === getChildAt(i)) {
                    return true
                }
            }
            return false
        }

    /**
     * Set the style of the RefreshView.
     *
     * @param refreshStyle One of [RefreshStyle.NORMAL]
     * , [RefreshStyle.PINNED], or [RefreshStyle.FLOAT]
     */
    @Suppress("Unused")
    fun setRefreshStyle(refreshStyle: RefreshStyle) {
        mRefreshStyle = refreshStyle
    }

    enum class RefreshStyle {
        NORMAL, PINNED, FLOAT
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    fun setOnRefreshListener(listener: OnRefreshListener?) {
        mOnRefreshListener = listener
    }

    interface OnRefreshListener {
        fun onRefresh()
    }

    /**
     * Per-child layout information for layouts that support margins.
     */
    class LayoutParams : MarginLayoutParams {
        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs)
        constructor(width: Int, height: Int) : super(width, height)
        constructor(source: ViewGroup.LayoutParams?) : super(source)
    }

    class DragDistanceConverter {
        /**
         * @param scrollDistance the distance between the ACTION_DOWN point and the ACTION_MOVE point
         * @param refreshDistance the distance between the refresh point and the start point
         * @return the real distance of the refresh view moved
         */
        fun convert(scrollDistance: Float, refreshDistance: Float): Float {
            val originalDragPercent = scrollDistance / refreshDistance
            val dragPercent = 1.0f.coerceAtMost(abs(originalDragPercent))
            val extraOS = abs(scrollDistance) - refreshDistance
            val tensionSlingshotPercent = 0f.coerceAtLeast(
                extraOS.coerceAtMost(refreshDistance * 2.0f) / refreshDistance
            )
            val tensionPercent = (tensionSlingshotPercent / 4 - (tensionSlingshotPercent / 4)
                .toDouble().pow(2.0)).toFloat() * 2f
            val extraMove = refreshDistance * tensionPercent * 2
            val convertY = (refreshDistance * dragPercent + extraMove).toInt()
            return convertY.toFloat()
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams): LayoutParams {
        return LayoutParams(p)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        return p is LayoutParams
    }

    companion object {
        private const val INVALID_INDEX = -1
        private const val INVALID_POINTER = -1

        //the animation duration of the RefreshView scroll to the refresh point or the start point
        private const val DEFAULT_ANIMATE_DURATION = 300

        // the threshold of the trigger to refresh
        private const val DEFAULT_REFRESH_TARGET_OFFSET_DP = 50
        private const val DECELERATE_INTERPOLATION_FACTOR = 2.0f
    }
}
