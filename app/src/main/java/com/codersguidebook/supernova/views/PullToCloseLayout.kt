package com.codersguidebook.supernova.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import androidx.customview.widget.ViewDragHelper.Callback
import kotlin.math.abs

class PullToCloseLayout(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private var listener: Listener? = null
    private val dragHelper: ViewDragHelper
    private var minFlingVelocity = 0f
    private var verticalTouchSlop = 0f

    init {
        val viewConfiguration = ViewConfiguration.get(context)
        minFlingVelocity = viewConfiguration.scaledMinimumFlingVelocity.toFloat()
        dragHelper = ViewDragHelper.create(this, ViewDragCallback(this))
    }

    override fun computeScroll() {
        super.computeScroll()
        if (dragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        var pullingDown = false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                verticalTouchSlop = event.y
                val dy = event.y - verticalTouchSlop
                if (dy > dragHelper.touchSlop) {
                    pullingDown = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - verticalTouchSlop
                if (dy > dragHelper.touchSlop) {
                    pullingDown = true
                }
            }
            MotionEvent.ACTION_UP -> verticalTouchSlop = 0f
        }
        if (!dragHelper.shouldInterceptTouchEvent(event) && pullingDown) {
            if (dragHelper.viewDragState == ViewDragHelper.STATE_IDLE &&
                dragHelper.checkTouchSlop(ViewDragHelper.DIRECTION_VERTICAL)
            ) {
                getChildAt(0)?.let {
                    dragHelper.captureChildView(it, event.getPointerId(0))
                    return dragHelper.viewDragState == ViewDragHelper.STATE_DRAGGING
                }
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        dragHelper.processTouchEvent(event!!)
        return dragHelper.capturedView != null
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    inner class ViewDragCallback(layout: PullToCloseLayout) : Callback() {
        private val pullToCloseLayout: PullToCloseLayout
        private var startTop = 0
        private var dragPercent = 0.0f
        private var isDismissed = false

        init {
            pullToCloseLayout = layout
        }

        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            return true
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            return if (top < 0) 0 else top
        }

        override fun onViewCaptured(view: View, activePointerId: Int) {
            startTop = view.top
            dragPercent = 0.0f
            isDismissed = false
        }

        override fun onViewPositionChanged(view: View, left: Int, top: Int, dx: Int, dy: Int) {
            val range = pullToCloseLayout.height
            val moved = abs(top - startTop)
            if (range > 0) {
                dragPercent = moved.toFloat() / range.toFloat()
            }
        }

        override fun onViewDragStateChanged(state: Int) {
            if (state == ViewDragHelper.STATE_DRAGGING) {
                pullToCloseLayout.listener?.pullToCloseIsDragging(true)
            } else pullToCloseLayout.listener?.pullToCloseIsDragging(false)

            if (isDismissed && state == ViewDragHelper.STATE_IDLE) {
                pullToCloseLayout.listener?.pullToCloseDismissed()
            }
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            isDismissed = dragPercent >= 0.50f ||
                    abs(xvel) > pullToCloseLayout.minFlingVelocity && dragPercent > 0.20f
            if (!isDismissed) pullToCloseLayout.dragHelper.settleCapturedViewAt(0, startTop)
            pullToCloseLayout.invalidate()
        }
    }

    interface Listener {
        /**
         * A method called when the layout has started being dragged or is released.
         *
         * @param dragging - A Boolean indicating whether the layout is being dragged (true)
         * or has been released (false)
         */
        fun pullToCloseIsDragging(dragging: Boolean)

        /** Layout is pulled down to dismiss */
        fun pullToCloseDismissed()
    }
}