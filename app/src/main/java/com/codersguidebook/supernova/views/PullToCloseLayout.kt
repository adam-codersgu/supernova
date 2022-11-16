package com.codersguidebook.supernova.views

import android.content.Context
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.customview.widget.ViewDragHelper
import androidx.customview.widget.ViewDragHelper.Callback
import kotlin.math.abs

class PullToCloseLayout(context: Context) : FrameLayout(context) {
    private val listener: PullToCloseLayout.Listener? = null
    private val dragHelper: ViewDragHelper
    private var minFlingVelocity = 0f
    private val verticalTouchSlop = 0f
    private val animateAlpha = false

    init {
        val viewConfiguration = ViewConfiguration.get(context)
        minFlingVelocity = viewConfiguration.scaledMinimumFlingVelocity.toFloat()
        dragHelper = ViewDragHelper.create(this, ViewDragCallback(this))
    }

    inner class ViewDragCallback(layout: PullToCloseLayout) : Callback() {
        private val pullToCloseLayout: PullToCloseLayout
        private var startTop = 0
        private var dragPercent = 0.0f
        private var capturedView: View? = null
        private var dismissed = false

        init {
            pullToCloseLayout = layout
        }

        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            return capturedView == null
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            return if (top < 0) 0 else top
        }

        override fun onViewCaptured(view: View, activePointerId: Int) {
            capturedView = view
            startTop = view.top
            dragPercent = 0.0f
            dismissed = false
        }

        override fun onViewPositionChanged(view: View, left: Int, top: Int, dx: Int, dy: Int) {
            val range: Int = pullToCloseLayout.height
            val moved = abs(top - startTop)
            if (range > 0) {
                dragPercent = moved.toFloat() / range.toFloat()
            }
            if (pullToCloseLayout.animateAlpha) {
                view.alpha = 1.0f - dragPercent
                pullToCloseLayout.invalidate()
            }
        }

        override fun onViewDragStateChanged(state: Int) {
            if (capturedView != null && dismissed && state == ViewDragHelper.STATE_IDLE) {
                pullToCloseLayout.removeView(capturedView)
                if (pullToCloseLayout.listener != null) {
                    pullToCloseLayout.listener.onDismissed()
                }
            }
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            dismissed = dragPercent >= 0.50f ||
                    abs(xvel) > pullToCloseLayout.minFlingVelocity && dragPercent > 0.20f
            val finalTop = if (dismissed) pullToCloseLayout.height else startTop
            pullToCloseLayout.dragHelper.settleCapturedViewAt(0, finalTop)
            pullToCloseLayout.invalidate()
        }
    }
}