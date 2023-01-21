package com.codersguidebook.supernova.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.codersguidebook.supernova.R
import com.google.android.material.color.MaterialColors

class RecyclerViewScrollbar(context: Context, attrs: AttributeSet) : View(context, attrs) {

    /*
    TODO:
        - Draw the scrollbar letter identifier
        - Set the width of the view
        - Only the track and thumb should respond to touch events. The identifier should not
        -
        - When scrolling, a listener interface should tell the fragment the position to scroll to
        -   Need to make sure that there is not conflict with the onScrollListener
        - For RecyclerView's who's content is not greater than the measuredHeight, the scrollbar should not be displayed. This could be evaluated in onDraw()?
        - Need to adjust the thumb height dynamically based on the contents of the RecyclerView. It must have a minumum value
        - The scrollbar should be invisible by default and only appear when the user is scrolling the recycler view
        - The scrollbar should hide upon scroll inactivity
        - The scrollbar should only respond to touch events when visible
        - The height of the thumb must never be greater than the height of the view
     */

    /*
    FIXME BUGS
        - It is often difficult to select the scrollbar. Maybe set the width of the scrollbar to larger than
        -   the width of the thumb/track? To avoid conflicts with the RecyclerView, it is important that the
        -   onTouch listener is only active when the scrollbar is visible.
     */

    /*
    FOR LIBRARY RELEASE:
       - Need to have the option to customise the colours of the scrollbar features (or at least match theme)
       - Also to customise the scrollbar (thumb + track) width
       - Look at sourcing all colours from the theme
     */

    private var listener: Listener? = null

    private var recyclerViewContentHeight: Int? = null
    private var recyclerViewScrollPosition = 0

    private val trackAndThumbWidth = 25
    private val minimumThumbHeight = trackAndThumbWidth * 4

    private val trackOffColour = ContextCompat.getColor(context, R.color.onSurface30)
    private var trackRect = Rect(trackAndThumbWidth, 0, 0, height)

    private val thumbOffColour = ContextCompat.getColor(context, R.color.onSurface84)
    private val thumbOnColour = MaterialColors.getColor(context, R.attr.colorSecondary, Color.CYAN)
    private var thumbRect = Rect(trackAndThumbWidth, 0, 0, getThumbHeight())
    private var thumbSelected = false

    private val valueLabel = ContextCompat.getDrawable(context, R.drawable.thumb_drawable)

    private var textHeight = 0f
    private val textColor = ContextCompat.getColor(context, R.color.blue7)

    private val textPaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = textColor
        if (textHeight == 0f) {
            // fixme: Logged 12.0
            textHeight = textSize
        } else {
            textSize = textHeight
        }
    }

    private val trackPaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = trackOffColour
    }

    private val thumbPaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = thumbOffColour
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.apply {
            // track?.draw(this)
            // drawText("A", 10f, 10f, textPaint)
            drawRect(trackRect, trackPaint)
            drawRect(thumbRect, thumbPaint)

            if (thumbSelected) {
                // TODO: Draw the value label
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != 0 && h != 0 && oldw == 0 && oldh == 0) {
            // fixme: test if the below is even necessary
            trackRect.set(trackAndThumbWidth, 0, 0, h)
            // fixme: if the height of the view changes, do we need to move the thumb?
        }
    }

    // fixme: test if the below is even necessary
    override fun getLayoutParams(): ViewGroup.LayoutParams {
        return super.getLayoutParams().apply {
            width = trackAndThumbWidth
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val y = event?.y ?: 0f

        when (event?.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                recyclerViewContentHeight?.let { height ->
                    val scrollProportion = y / measuredHeight
                    val newScrollPosition = scrollProportion * height
                    listener?.onScrollTo(newScrollPosition.toInt())
                }
                thumbSelected = true
                thumbPaint.color = thumbOnColour
                return true
            }
            else -> {
                thumbSelected = false
                thumbPaint.color = thumbOffColour
            }
        }

        return super.onTouchEvent(event)
    }

    /** Move the thumb along the track to reflect the RecyclerView scroll position */
    private fun updateScrollPosition() {
        recyclerViewContentHeight?.let { height ->
            // The scroll proportion will be between 0 (top) and 1 (bottom)
            val scrollProportion = recyclerViewScrollPosition.toFloat() / height
            // The scroll position will be the height of the scrollbar * the scroll proportion
            val scrollPosition = measuredHeight * scrollProportion

            // Update the y coordinate of the thumb to reflect the scroll position
            // The y coordinate should not cause the thumb to go off the screen,
            // and so maximum y coordinate values are provided as a fallback.
            val maximumTopValue = measuredHeight - getThumbHeight()
            val topValueToUse = if (scrollPosition > maximumTopValue) maximumTopValue
            else scrollPosition.toInt()

            val proposedBottomValue = scrollPosition + getThumbHeight()
            val bottomValueToUse = if (proposedBottomValue > measuredHeight) measuredHeight
            else proposedBottomValue.toInt()

            thumbRect.set(trackAndThumbWidth, topValueToUse, 0, bottomValueToUse)
            invalidate()
        }
    }

    /**
     * Determine the appropriate height of the scrollbar thumb.
     *
     * @return An integer representing the height to use for the thumb. The height
     * will always be equal to or greater than $minimumThumbHeight
     */
    private fun getThumbHeight(): Int {
        // TODO: Need to update the thumb height to reflect the contents size of the RecyclerView
        val thumbHeight = 0
        return if (thumbHeight > minimumThumbHeight) thumbHeight
        else minimumThumbHeight
    }

    /**
     * Set the height of the RecyclerView's contents.
     *
     * @param height The cumulative total height of the RecyclerView's contents.
     */
    fun notifyRecyclerViewContentHeightChanged(height: Int) {
        if (height != recyclerViewContentHeight && height >= 0) {
            recyclerViewContentHeight = height
        }
    }

    /**
     * Set the RecyclerView scroll position and move the scrollbar thumb accordingly.
     *
     * @param position The scroll position.
     */
    fun notifyRecyclerViewScrollPositionChanged(position: Int) {
        recyclerViewScrollPosition = position
        updateScrollPosition()
    }

    interface Listener {
        /**
         * A method called when the scrollbar thumb is being dragged.
         *
         * @param position The position that the user has scrolled to.
         */
        fun onScrollTo(position: Int)
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }
}