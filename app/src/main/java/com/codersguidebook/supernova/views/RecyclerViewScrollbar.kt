package com.codersguidebook.supernova.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.codersguidebook.supernova.R
import com.google.android.material.color.MaterialColors
import kotlin.math.roundToInt

class RecyclerViewScrollbar(context: Context, attrs: AttributeSet) : View(context, attrs) {

    /*
    TODO:
        - Draw the scrollbar letter identifier
        - Set the width of the view
        - Only the track and thumb should respond to touch events. The identifier should not
        -
        - When scrolling, a listener interface should tell the fragment the position to scroll to
        -   Need to make sure that there is not conflict with the onScrollListener
        - The scrollbar should be invisible by default and only appear when the user is scrolling the recycler view
        - The scrollbar should hide upon scroll inactivity
        - The scrollbar should only respond to touch events when visible
        - Add custom properties as described here https://developer.android.com/develop/ui/views/layout/custom-views/create-view
        - The properties should include those specified in the 'FOR LIBRARY RELEASE' comment
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
       - Property to set the minimum thumb height
       - Look at sourcing all colours from the theme

       BENEFITS OF THE LIBRARY:
       - The scrollbar thumb always has a minimum height (unlike the default fast scroll thumb, which
       can become too small when the RecyclerView has lots of content. This is a known issue that has been
       unresolved for years https://issuetracker.google.com/issues/64729576)
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

    private val valueLabelWidthAndHeight = 200f
    private val valueLabelCornerOffset = valueLabelWidthAndHeight / 4
    private val valueLabelCornerMidway = valueLabelCornerOffset / 5
    private var valueLabelPath = updateValueLabelPath()

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

        // If the RecyclerView is not larger than the View, then no need to display the scrollbar
        if (measuredHeight >= (recyclerViewContentHeight ?: measuredHeight)) return

        canvas.apply {
            // Draw the track and thumb
            val savedState = save()
            translate((width - trackAndThumbWidth).toFloat(), 0f)
            drawRect(trackRect, trackPaint)
            drawRect(thumbRect, thumbPaint)
            restoreToCount(savedState)

            if (thumbSelected) {
                drawPath(valueLabelPath, thumbPaint)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != 0 && h != 0) {
            trackRect.set(trackAndThumbWidth, 0, 0, h)
        }
    }

    override fun getLayoutParams(): ViewGroup.LayoutParams {
        return super.getLayoutParams().apply {
            width = trackAndThumbWidth + valueLabelWidthAndHeight.roundToInt()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val y = event?.y ?: 0f

        when (event?.action) {
            // Action down = 0; Action move = 2;
            ACTION_DOWN, ACTION_MOVE -> {
                recyclerViewContentHeight?.let { height ->
                    val scrollProportion = y / measuredHeight
                    val newScrollPosition = scrollProportion * height
                    listener?.onScrollTo(newScrollPosition.toInt())
                }
                thumbSelected = true
                thumbPaint.color = thumbOnColour
                return true
            }
            // Action cancel = 3; Action up = 1;
            ACTION_CANCEL, ACTION_UP -> {
                thumbSelected = false
                thumbPaint.color = thumbOffColour
                invalidate()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    /** Move the thumb along the track to reflect the RecyclerView scroll position */
    private fun updateScrollPosition() {
        recyclerViewContentHeight?.let { height ->
            // The scroll proportion will be between 0 (top) and 1 (bottom)
            val scrollProportion = recyclerViewScrollPosition.toFloat() / height
            // The scroll position will be the height of the View * the scroll proportion
            val scrollPosition = measuredHeight * scrollProportion

            updateThumbRect(scrollPosition)
            if (thumbSelected) updateValueLabelPath(scrollPosition)

            invalidate()
        }
    }

    /**
     * Update the scrollbar thumb's Rect coordinates to reflect the updated scroll position.
     *
     * @param scrollPosition The position on the Y axis that the thumb should be positioned.
     */
    private fun updateThumbRect(scrollPosition: Float) {
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
    }

    /**
     * Update the scrollbar value label's Path coordinates to reflect the updated scroll position.
     *
     * @param scrollPosition The position on the Y axis that the value label should be positioned.
     * Default value = 0f.
     */
    private fun updateValueLabelPath(scrollPosition: Float = 0f): Path {
        // TODO: This whole method needs to be rewritten. Note you need to take into account
        //  the value label's dimensions and ensure it does not fall outside the View when drawn

        /*
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
        */
        return Path().apply {
            moveTo(valueLabelWidthAndHeight, valueLabelWidthAndHeight)
            lineTo(valueLabelWidthAndHeight, 0f + valueLabelCornerOffset)

            // Draw the top right corner
            val topRightX1 = valueLabelWidthAndHeight - (valueLabelCornerOffset / 2)
            val topRightX2 = valueLabelWidthAndHeight - valueLabelCornerOffset
            quadTo(topRightX1, valueLabelCornerMidway, topRightX2, 0f)

            // Draw the top left corner
            lineTo(valueLabelCornerOffset, 0f)
            quadTo(valueLabelCornerMidway, valueLabelCornerMidway, 0f, valueLabelCornerOffset)

            // Draw the bottom left corner
            lineTo(0f, valueLabelWidthAndHeight - valueLabelCornerOffset)
            val bottomLeftY1 = valueLabelWidthAndHeight - valueLabelCornerMidway
            quadTo(valueLabelCornerMidway, bottomLeftY1, valueLabelCornerOffset, valueLabelWidthAndHeight)

            // fixme: Is the below necessary?
            // Complete the value label
            lineTo(valueLabelWidthAndHeight, valueLabelWidthAndHeight)
        }
    }

    /**
     * Determine the appropriate height of the scrollbar thumb.
     *
     * @return An integer representing the height to use for the thumb. The height
     * will always be equal to or greater than $minimumThumbHeight
     */
    private fun getThumbHeight(): Int {
        val viewHeightProportionOfContentHeight = measuredHeight.toFloat() /
                (recyclerViewContentHeight ?: measuredHeight)
        val thumbHeight = measuredHeight * viewHeightProportionOfContentHeight
        return when {
            thumbHeight > measuredHeight -> measuredHeight
            thumbHeight > minimumThumbHeight -> thumbHeight.roundToInt()
            else -> minimumThumbHeight
        }
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