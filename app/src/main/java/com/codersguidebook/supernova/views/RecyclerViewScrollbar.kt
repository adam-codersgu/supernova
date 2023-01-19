package com.codersguidebook.supernova.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.codersguidebook.supernova.R

class RecyclerViewScrollbar(context: Context, attrs: AttributeSet) : View(context, attrs) {

    /*
    TODO:
        - Draw the thumb
        - Draw the scrollbar letter identifier
        - Set the width of the view
        - Only the track and thumb should respond to touch events. The identifier should not
        -
        - The scrollbar should be invisible by default and only appear when the user is scrolling the recycler view
        - The scrollbar should hide upon scroll inactivity
        - The scrollbar should only respond to touch events when visible
     */

    /*
    The width of the track and thumb should be 30
     */

    private val thumb = ContextCompat.getDrawable(context, R.drawable.thumb_drawable)

    private val track = ContextCompat.getDrawable(context, R.drawable.line_drawable)
    private val trackWidth = 30
    private val trackOffColour = ContextCompat.getColor(context, R.color.onSurface30)
    private var trackRect = Rect(trackWidth, 0, 0, height)

    private var textHeight = 0f
    private val textColor = ContextCompat.getColor(context, R.color.blue7)

    private val textPaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = textColor
        if (textHeight == 0f) {
            Log.e("DEBUGGING", "The text size is $textSize")
            textHeight = textSize
        } else {
            textSize = textHeight
        }
    }

    private val trackPaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = trackOffColour
        if (textHeight == 0f) {
            Log.e("DEBUGGING", "The text size is $textSize")
            textHeight = textSize
        } else {
            textSize = textHeight
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // setBackgroundColor(trackOffColour)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.apply {
            //track?.draw(this)
            //drawText("A", 10f, 10f, textPaint)
            drawRect(trackRect, trackPaint)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != 0 && h != 0 && oldw == 0 && oldh == 0) {
            // fixme: test if the below is even necessary
            trackRect.set(trackWidth, 0, 0, h)
        }
    }

    // fixme: test if the below is even necessary
    override fun getLayoutParams(): ViewGroup.LayoutParams {
        return super.getLayoutParams().apply {
            width = trackWidth
        }
    }
}