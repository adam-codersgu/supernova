package com.codersguidebook.supernova.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.codersguidebook.supernova.R

class RecyclerViewScrollbar(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val thumb = ContextCompat.getDrawable(context, R.drawable.thumb_drawable)
    private val track = ContextCompat.getDrawable(context, R.drawable.line_drawable)

    private var textHeight = 0f
    private val textColor = ContextCompat.getColor(context, R.color.blue7)

    private val textPaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = textColor
        if (textHeight == 0f) {
            textHeight = textSize
        } else {
            textSize = textHeight
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            track?.draw(this)
            drawText("A", 10f, 10f, textPaint)
        }
    }

    // fixme: test if the below is even necessary
    override fun getLayoutParams(): ViewGroup.LayoutParams {
        return super.getLayoutParams().apply {
            width = 50
        }
    }
}